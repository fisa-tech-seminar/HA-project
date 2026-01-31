
## 🚀 MySQL HA & 모니터링 환경 실행 가이드

이 프로젝트는 **MySQL 고가용성(HA)**, **ProxySQL(읽기/쓰기 분리)**, 그리고 **Prometheus/Grafana 모니터링** 환경을 한 번에 구축합니다.

### 1. 사전 준비 (Prerequisites)

* **Docker & Docker Compose**가 설치되어 있어야 합니다.
* 기존에 로컬에서 실행 중인 MySQL(3306 포트)이 있다면 중지해 주세요.

### 2. 실행 방법 (Step-by-Step)

터미널에서 `infra` 폴더로 이동한 후 다음 명령어를 실행하세요.

```bash
# 1. 인프라 컨테이너 일괄 실행
docker compose up -d

# 2. 실행 상태 확인 (모든 컨테이너가 Up 또는 Healthy인지 확인)
docker compose ps

```

---

### 3. 정상 동작 확인 방법 (Checklist)

팀원들이 각 서비스가 잘 돌아가는지 확인할 수 있는 체크포인트입니다.

#### ① DB 접속 및 복제 확인

* **ProxySQL 접속 (애플리케이션용):** `localhost:3307` (User: `appuser` / PW: `apppass`)
* Spring 프로젝트의 `application.yml` 설정을 이 포트로 변경하세요.


* **복제 상태 확인:** `mysql_replica` 컨테이너 로그에서 아래 문구가 보이면 성공입니다.
```text
Replica receiver thread ... connected to source 'repl@mysql_primary:3306'

```
#### ② Orchestrator 에서 DB Discover
infra 폴더에서 아래의 명령어를 실행해야 Orchestrator UI에서 DB 토폴로지를 확인할 수 있습니다.
```
docker exec -it orchestrator orchestrator-client -c discover -i mysql_primary

```

#### ③ 관리 및 모니터링 UI 접속

브라우저를 열어 아래 주소들이 정상적으로 뜨는지 확인하세요.

| 서비스 | 접속 주소 (URL) | 주요 확인 사항 |
| --- | --- | --- |
| **Grafana** | `http://localhost:3000` | DB 부하 및 지표 확인 (ID/PW: `admin` / `admin`) |
| **Orchestrator** | `http://localhost:3001` | DB 토폴로지(Master-Replica 구조) 시각적 확인 |
| **Prometheus** | `http://localhost:9090` | `Status > Targets`에서 모든 Exporter가 `UP`인지 확인 |

---

### 4. 핵심 시연 시나리오 (Failover 테스트)

팀원들과 고가용성을 테스트해보고 싶다면 아래 순서를 따라 하세요.

1. **장애 발생:** `docker stop mysql_primary` 명령어로 마스터 DB를 강제로 끕니다.
2. **자동 복구:** **Orchestrator UI(3001번 포트)**에서 복구 버튼을 눌러 `mysql_replica`를 새로운 Master로 승격시키는 과정을 관찰합니다.
3. **서비스 유지:** 마스터가 바뀌어도 **ProxySQL(3307번 포트)**을 통한 DB 접속은 끊기지 않고 정상 유지되는지 확인합니다.

---

### ⚠️ 주의 사항

* **초기 구동 시간:** `orchestrator-db`가 건강검진(Healthcheck)을 완료할 때까지 약 50~200초 정도 소요될 수 있습니다. Orchestrator UI가 바로 안 뜨더라도 잠시만 기다려 주세요.
* **GTID 경고:** UI에 노란색 GTID 경고가 뜰 수 있으나, 이는 초기화 과정에서의 미세한 차이일 뿐 Failover 작동에는 문제가 없습니다.

---