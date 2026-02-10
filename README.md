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

## 📖Project Overview

금융/결제 도메인인 **Digital Wallet(디지털 지갑)** 서비스를 기반으로, **단일 장애점(SPoF)이 없는 고가용성 인프라**를 구축하고 검증한 프로젝트입니다.

단순한 기능 구현을 넘어, 대규모 트래픽 환경에서 필수적인 **데이터 정합성, 멱등성, 그리고 DB 고가용성을 위한 DB Failover** 과정을 엔지니어링 관점에서 구현했습니다.

### 🎯 **Key Objectives**

- **Infrastructure:** MySQL Replication, ProxySQL, Orchestrator를 활용한 **자동화된 HA 환경 구축**
- **Failover Validation:** 장애 발생 시 복구 프로세스 시각화
    - Replication(이중화) → Failover(승격) → Observability(관측)
- **Data Consistency:** 동시성 이슈 제어를 위한 **Pessimistic Lock** 및 중복 결제 방지를 위한 **Idempotency Key** 적용

---

## 📸 Architecture Overview

애플리케이션은 DB 토폴로지를 알 필요 없이 **단일 엔드포인트(ProxySQL)** 만 바라보며, HA와 Failover는 인프라 레이어에서 전담하는 구조입니다.

<img width="744" height="522" alt="image" src="https://github.com/user-attachments/assets/af85695b-822c-48e9-9855-2d513fc22c16" />


- **Application Layer:** Spring Boot API (Wallet Service)
- **Proxy Layer:** ProxySQL (Read/Write Splitting & Failover Routing)
- **Database Layer:** MySQL 8.0 GTID Replication (1 Primary - 2 Replicas)
- **Management Layer:** Orchestrator (Topology Management & Auto-Failover)
- **Monitoring Layer:** Prometheus & Grafana

---

## 🚀 Scenario: “Non-Stop Digital Wallet” (HA Validation)

**DB가 죽어도 서비스는 멈추지 않는다**는 것을 검증하기 위한 핵심 시나리오입니다.

실제 운영 환경을 모사하여 Primary DB에 장애를 유발하고 시스템을 복구하는 과정을 검증했습니다.

[시연 GIF]

1. **Normal State:** 트래픽이 ProxySQL을 통해 Primary DB로 정상 유입
2. **Failure Injection:** **“docker stop mysql_primary”** 명령어로 강제 장애 발생
3. **Detection & Decision:** Orchestrator가 장애 감지 및 관리자 Slack 알림
    
     관리자가 Grafana 대시보드에서 Replication Lag 및 Replica DB 상태 확인 후 Failover 실행
    
4. **Failover:** Orchestrator가 최적의 Replica DB를 새로운 Master DB로 승격
5. **Routing Update:** ProxySQL이 토폴로지 변경을 감지하고 트래픽을 새로운 Master DB로 즉시 전환
6. **Recovery:** 모니터링으로 서비스 복구 이후 결제/충전 기능 정상 동작 확인

---

## 📊 Monitoring & Observability

장애 감지(MTTD)와 복구(RTO)를 수치로 증명하기 위해 정밀한 모니터링 대시보드를 구축했습니다.

<img width="1915" height="871" alt="image" src="https://github.com/user-attachments/assets/b490affc-f8a5-42b5-9baf-41947dbf551f" />


- **Failover Detection:** read_only 상태 변화를 감지하여 Master 승격 시점 시각화
- **Service Impact:** Total QPS의 V자 반등 곡선을 통해 트래픽 복구 확인
- **Data Safety:** Replication Lag 및 Aborted Connections 모니터링을 통한 데이터 유실 가능성 차단

---

## 🛠️ Core Features

### 1. Application Layer (Stability)

- **동시성 제어 (Concurrency Control):**
    - `SELECT ... FOR UPDATE` (Pessimistic Lock)을 사용하여 충전/결제 시 Race Condition 원천 차단
- **멱등성 보장 (Idempotency):**
    - 네트워크 타임아웃/재시도 상황에서 중복 결제를 막기 위해 Idempotency-Key 기반의 중복 요청 필터링 구현
        

### 2. Infrastructure Layer (High Availability)

- **MySQL GTID Replication:**
    - 데이터 일관성을 위한 GTID 기반 비동기 복제 구성
- **ProxySQL Routing:**
    - 쿼리 패턴에 따른 Read/Write Splitting (쓰기: Master, 읽기: Replica 분산)
- **Orchestrator-Assisted Failover:**
    - 완전 자동화의 위험성을 배제하기 위해 모니터링 지표(Lag, Thread Status) 확인 후 운영자가 승인하는 'One-Click Failover' 프로세스 구축
    - 복구 자체는 Orchestrator가 수행하되, 의사결정(Decision Making)은 데이터에 기반하여 수행

---

## 🔧 Trouble Shooting

### 1. MySQL Replication 설정 자동화 및 Race Condition 해결

- **Problem:** docker-compose로 다중 컨테이너 실행 시, MySQL 컨테이너가 완전히 구동되기 전에 복제 설정 명령어가 실행되어 연결 실패 및 엔드포인트 불일치 문제가 지속적으로 발생
  
- **Process:**
    - 초기에는 command 옵션과 depends_on 옵션으로 해결하려 했지만 컨테이너 초기화 속도 차이로 인한 **Race Condition**을 제어하기 어려움
    - 수동 설정은 휴먼 에러를 유발하므로 배제
      
- **Solution:** **'Setup Container(Sidecar Pattern)'** 도입
  
    별도의 일회성 컨테이너를 추가하여, Master/Replica의 헬스 체크를 선행하고 이후 복제 설정까지 하는 쉘 스크립트를 실행하도록 구성
  
- **Outcome:** `docker compose up -d` 명령어 하나 만으로 Master-Replica 이중화 환경이 완벽하게 구성되도록 자동화 설정

<br>

### 2. HAProxy(L4)의 한계와 ProxySQL(L7) 도입을 통한 정합성 확보

- **Problem:** 초기에 도입한 HAProxy는 **TCP(Layer 4) 기반**으로 동작하여 쿼리의 내용을 분석해 라우팅하지 못하는 문제 발생
단순 Port 기반 분산은 가능했으나, 동시성 제어를 위한 `SELECT ... FOR UPDATE` 쿼리까지 Replica로 분산되어 Lock이 동작하지 않는 치명적인 **정합성 이슈 발생**

- **Process:**
    - 외부 스크립트를 통해 read_only 값을 체크하여 라우팅하려 했으나, 구조가 복잡해지고 관리 포인트가 늘어나는 단점 존재
    - 쿼리 내용을 분석하여 라우팅 할 수 있는 **Layer 7 Database Proxy**의 필요성 확인
      
- **Solution:** **ProxySQL**로 기술 스택 변경 및 **Query Rules** 적용
    - **Rule 1:** `^SELECT.*FOR UPDATE$` 정규식을 매칭하여 강제로 **Hostgroup 10(Writer)** 로 라우팅
    - **Rule 2:** 일반 `^SELECT` 쿼리는 **Hostgroup 20(Reader)** 로 라우팅하여 부하 분산
      
- **Outcome:** 애플리케이션 코드 수정 없이 인프라 레벨에서 **Read/Write Splitting**과 **Transactional Consistency**를 동시에 달성

---

## 📦 Tech Stack

| **Category** | **Technology** |
| --- | --- |
| **Language & Framework** | Java 17, Spring Boot 3.5, Spring Data JPA, Hibernate |
| **Database** | MySQL 8.0 (GTID Replication) |
| **HA & Routing** | **ProxySQL**, **Orchestrator** |
| **Monitoring** | **Prometheus**, **Grafana**, mysqld_exporter |
| **Infrastructure** | Docker, Docker Compose |

---

## 🚀 Getting Started

이 프로젝트는 **Docker Compose**를 통해 로컬 환경에서 즉시 실행 가능합니다.

### Prerequisites

- Docker & Docker Compose installed
- Git installed

### Installation & Run

```bash
# 1. Repository Clone
git clone https://github.com/fisa-tech-seminar/HA-project.git

# 2. Execute (All-in-One)
# MySQL(Primary/Replica), ProxySQL, Orchestrator, App, Monitoring이 모두 실행됩니다.
docker-compose up -d

# 3. Initialize Topology (Orchestrator Discovery) 컨테이너가 정상적으로 실행된 후(약 20~30초 대기), Orchestrator가 DB 클러스터를 인식하도록 아래 명령어를 실행합니다.
# Orchestrator에게 Primary DB를 찾아보라고(Discover) 명령
docker exec -it orchestrator orchestrator-client -c discover -i mysql_primary
```

### Access Points

- **Wallet API:** `http://localhost:8080`
- **Grafana Dashboard:** `http://localhost:3000` (ID/PW: `admin`/`admin`)
- **Orchestrator UI:** `http://localhost:3001`
- **ProxySQL Admin:** `mysql -u admin -p... -h 127.0.0.1 -P 6032`


---

<details> <summary><b>📂 Domain Model & API Specs (Click to Expand)</b></summary>
  
## Domain Model

**Wallet (지갑):** 사용자의 잔액(Balance) 상태 관리

- `user_id (PK)`, `balance`, `version`

**Wallet_tx (거래 원장):** 모든 자금 흐름의 불변 기록 (Ledger)

- `tx_id (PK)`, `type (TOPUP/PAY)`, `amount`, `status`, `idempotency_key (UNIQUE)`

---

## 📊 ERD

<img width="1026" height="252" alt="Image" src="https://github.com/user-attachments/assets/2ff631ee-9d0b-4d7f-ad36-99d77a334c9c" />

---

## API (Summary)

Base path: `/api/v1`

| **Method** | **URI** | **Description** |
| --- | --- | --- |
| `GET` | `/wallets/{userId}` | 잔액 조회 |
| `POST` | `/wallets/{userId}/topup` | 잔액 충전 (Header: `Idempotency-Key` 필수) |
| `POST` | `/wallets/{userId}/pay` | 결제 (Header: `Idempotency-Key` 필수) |
</details>

---



## 🤝 Collaboration

| **1. Issue & PR Workflow** | **2. Knowledge Base** | **3. AI Code Review** |
| --- | --- | --- |
| <img width="916" height="873" alt="image" src="https://github.com/user-attachments/assets/2009d162-25ee-4c7e-b01f-b12f1993656c" /> | <img width="828" height="907" alt="image" src="https://github.com/user-attachments/assets/ac3a2a50-1d1d-4984-9c68-84ec0092f4a8" /> | <img width="881" height="821" alt="image" src="https://github.com/user-attachments/assets/f2fbbfa9-528b-4b11-941a-219209e5a0a7" /> |
| **Git Flow & Convention**<br>이슈(Issue) 단위로 브랜치를 관리하고,<br>PR 승인 절차를 거쳐 Merge하는<br>엄격한 Git Flow 전략 준수 | **Notion 아카이빙**<br>개발 일정, 회의록, 트러블 슈팅 등<br>프로젝트의 모든 기술적 자산을<br>노션 페이지에 중앙화하여 공유 | **CodeRabbit 활용**<br>AI 코드 리뷰 도구를 도입하여<br>PR 생성 즉시 코드 스타일 및<br>잠재적 버그를 1차 자동 검증 |

---


<br/>
