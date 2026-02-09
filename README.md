# 🏷️ HA-project (DB 이중화)

> **Digital Wallet DB High Availability Demo** 💰
> 

<br>

## 🧑‍💻 팀원 소개
| ![](https://avatars.githubusercontent.com/u/181299322?v=4) | ![](https://avatars.githubusercontent.com/u/89902255?v=4) | ![](https://avatars.githubusercontent.com/u/204296918?v=4) |![](https://avatars.githubusercontent.com/u/180767288?v=4)|
|:---:|:---:|:---:|:---:|
| **신성혁**<br>[@ShinSungHyuk](https://github.com/ssh221) | **유예원**<br>[@Yewon0106](https://github.com/Yewon0106) | **이준호**<br>[@Junhoss](https://github.com/Junhoss) | **조민우**<br>[@MinWoo Cho](https://github.com/minwoo-00) |

<br/>

---

## 💡프로젝트 개요

**Digital Wallet(디지털 지갑)** 도메인을 기반으로, DB 고가용성(HA) 환경에서 발생하는 핵심 이슈를 재현하고 검증하는 데모 진행

- **애플리케이션 레이어**
    - 지갑 API(잔액 조회/충전/결제)로 구성
- **인프라 레이어**
    - **MySQL Replication**, **ProxySQL** 단일 엔드포인트, **Orchestrator** 기반 Failover, **Prometheus/Grafana** 모니터링으로 구성


<br>


## 🔎 프로젝트 목표

- **DB HA**의 핵심 구성요소를 단일 데모로 연결
    - **Replication(이중화) → Failover(승격) → Observability(관측)**
- 금융/결제 서비스에서 요구되는 트랜잭션 특성 반영
    - 동시성 상황에서 잔액 정합성 유지
    - 재시도/중복 요청에 대한 멱등 처리
    - 거래 원장(ledger) 기록

<br>


## 🛠️ Tech Stack

- **Application**: Spring Boot, Spring Data JPA
- **Database**: MySQL (Replication)
- **HA / Routing**: ProxySQL, Orchestrator
- **Monitoring**: Prometheus, Grafana, exporters
- **Infra**: Docker

<br>

<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> ![MySQL](https://img.shields.io/badge/mysql-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white) 
<img src="https://img.shields.io/badge/ProxySQL-004088?style=for-the-badge&logo=&logoColor=white"> <img src="https://img.shields.io/badge/Orchestrator-black?style=for-the-badge&logo=&logoColor=white">
![Grafana](https://img.shields.io/badge/grafana-%23F46800.svg?style=for-the-badge&logo=grafana&logoColor=white) 	![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=Prometheus&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

<br>

---

## 📍 Core Features

### 1️⃣ Wallet API

- 잔액 조회
- 충전(Top-up)
- 결제(Pay)
- 거래 원장(`wallet_tx`) 기록
    - 거래 유형/금액/상태/멱등키/거래 후 잔액 스냅샷 등

> “현재 잔액(wallet)”과 “거래 내역(wallet_tx)”을 분리하여 상태(state)와 이벤트(event) 구분
> 

<br>


### 2️⃣ Consistency: Pessimistic Lock

동일 사용자 지갑에 대한 동시 요청이 들어와도 잔액이 깨지지 않도록 구현

- `SELECT ... FOR UPDATE` 기반으로 **wallet row lock** 획득
- 트랜잭션 범위 내에서 잔액 업데이트 및 원장 기록 처리

<br>

### 3️⃣ Idempotency: Idempotency-Key

네트워크 타임아웃/재시도/중복 호출 상황에서 중복 결제 방지

- 요청 헤더의 `Idempotency-Key`를 기준으로 동일 요청 재전송 시 기존 결과를 재사용
- DB 레벨에서 `wallet_tx.idempotency_key` UNIQUE 제약으로 중복 저장을 방지

<br>

### 4️⃣ DB High Availability (Infra Layer)

애플리케이션은 DB 토폴로지를 직접 알 필요가 없도록 인프라 레이어에서 HA를 담당

- **MySQL Primary–Replica Replication**
    - Primary에서 발생한 변경을 Replica로 전파
    - GTID 기반 비동기 복제
- **ProxySQL**
    - 애플리케이션은 **단일 DB 엔드포인트**로만 연결
    - (구성에 따라) 읽기/쓰기 라우팅
    - `FOR UPDATE` 쿼리는 반드시 Writer(Primary)로 처리되어야 함
- **Orchestrator**
    - 토폴로지 관찰 및 장애 시 Replica 승격(Promote)을 통한 Failover

<br>


### 5️⃣ Observability: Prometheus / Grafana

장애/복구 과정을 “눈으로” 확인하기 위해 모니터링 스택 포함

- Prometheus: Exporter 기반 지표 수집
- Grafana: DB/호스트/컨테이너 지표 시각화 및 장애 감지 시 알림 설정
- 장애 발생(Primary down) 및 회복(Failover) 시점의 변화를 관찰


<br>

---

## 📸 Architecture

<br>

<img width="744" height="522" alt="Image" src="https://github.com/user-attachments/assets/7827dce1-97a2-4c5c-88e8-9cdc233d0f88" />

<br>
<br>

## 📊 ERD

<img width="1026" height="252" alt="Image" src="https://github.com/user-attachments/assets/2ff631ee-9d0b-4d7f-ad36-99d77a334c9c" />

<br>
<br>

## 📚 API (Summary)

Base path: `/api/v1`

- `GET /wallets/{userId}` — balance 조회
- `POST /wallets/{userId}/topup` — 충전
- `POST /wallets/{userId}/pay` — 결제

요청 공통:

- Header: `Idempotency-Key: <uuid>`
- Body: `{ "amount": <number> }`

<br>

---

## 👨‍🏫 Scenario: “Non-Stop Digital Wallet” (HA Validation)

이 시나리오는 시스템의 “정상 동작 → 장애 발생 → Failover → 회복” 흐름을 검증합니다.
<img width="546" height="486" alt="Image" src="https://github.com/user-attachments/assets/fe960584-f868-4501-b645-8a3cfcb3b2e1" />
