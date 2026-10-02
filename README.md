# Oneport 플랫폼 시연용 앱

공통 배포 플랫폼(`oneport-platform`)이 수행하는 배포 기능(최초 배포, 블루그린·카나리 전환, DB 마이그레이션, 자동 롤백, 앱별 로그)을 확인하기 위한 앱입니다. 같은 API를 두 프레임워크로 구현했습니다.

| 디렉터리 | 프레임워크 | app-id | 주소 |
| --- | --- | --- | --- |
| `notes-fastapi/` | Python 3.13, FastAPI, SQLAlchemy 2.0, Alembic | `notes-py` | `https://notes-py.app.yubin.dev` |
| `notes-spring/` | Java 25, Spring Boot 4.1, Spring Data JPA, Flyway | `notes-java` | `https://notes-java.app.yubin.dev` |

앱 기능은 노트 목록·추가·삭제입니다. 화면 상단 배너가 실행 중인 버전, git SHA, 인스턴스 이름을 1초마다 갱신해 표시하므로 트래픽이 어느 버전으로 가는지 바로 확인할 수 있습니다.

## API 계약

두 앱은 같은 경로와 응답 형식을 제공합니다. 응답 필드는 snake_case입니다.

| 경로 | 응답 | 용도 |
| --- | --- | --- |
| `GET /health` | `200 {"status":"ok"}`. DB를 확인하지 않습니다. `APP_FORCE_UNHEALTHY=true`면 `503` | liveness. ALB·Cloud Run health check, 자동 롤백 시연 |
| `GET /ready` | DB `SELECT 1` 성공 시 `200 {"status":"ok","database":"up"}`, 실패 시 `503` | readiness |
| `GET /version` | `{"app","version","git_sha","built_at","color","hostname","started_at"}` | 릴리즈 식별, 카나리 비율 확인 |
| `GET /` | 시연 화면 (HTML) | 브라우저로 확인 |
| `GET /notes?limit=50` | `[{"id","title","created_at"}]` 최신순 (`limit` 1~200) | DB 읽기 |
| `POST /notes` | 본문 `{"title"}` → `201` 생성된 노트. 공백·200자 초과는 `400`(Spring) / `422`(FastAPI) | DB 쓰기 |
| `DELETE /notes/{id}` | `204`. 없는 id는 `404` (`note_id` 포함) | DB 쓰기 |
| `GET /failure` | 항상 `503` | 실패 응답 감지 검증 |

응답 헤더 `X-Request-ID`에 요청 ID가 들어갑니다. 클라이언트가 UUID 형식으로 보내면 그대로 쓰고, 없거나 형식이 다르면 새로 발급합니다. 같은 값이 로그의 `request_id` 필드에 기록됩니다.

## 환경변수

플랫폼이 컨테이너에 주입합니다. AWS는 ECS task definition의 `environment`와 `secrets`, GCP는 Cloud Run의 env와 Secret Manager 참조로 전달합니다.

| 변수 | 값 | 비고 |
| --- | --- | --- |
| `PORT` | 기본 `8080` | Cloud Run이 지정하는 이름과 같습니다 |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` | DB 접속 정보 | AWS는 RDS endpoint, GCP는 Cloud SQL private IP |
| `DB_PASSWORD` | DB 비밀번호 | 시크릿으로 주입합니다 |
| `DATABASE_URL` | `postgresql://user:pass@host:port/db` | FastAPI만 지원. 지정하면 `DB_*` 대신 사용합니다 |
| `APP_VERSION`, `GIT_SHA`, `BUILT_AT` | 이미지 빌드 시 `--build-arg`로 고정 | `/version`에 노출됩니다 |
| `APP_COLOR` | CSS 색상 | 배너 색. 버전별 기본값을 이미지에 두고 환경변수로 바꿀 수 있습니다 |
| `APP_FORCE_UNHEALTHY` | `true`/`false` | `true`면 `/health`가 503을 반환합니다 |
| `LOG_FORMAT` | `json`(기본) 또는 `text` | 플랫폼에서는 `json`, 로컬에서는 `text` |
| `LOG_LEVEL` | 기본 `INFO` | |

## 로그

두 앱 모두 stdout에 한 줄 JSON으로 기록합니다. 모든 로그에 `request_id`가 들어가고, FastAPI는 `app`·`version` 필드를, Spring은 ECS 형식의 `service.name`·`service.version` 필드를 함께 기록합니다. 요청 로그는 `method`, `path`, `status`, `duration_ms`를 포함하며 `/health`, `/ready`는 기록하지 않습니다.

## DB 마이그레이션

마이그레이션은 컨테이너 시작 시 서버보다 먼저 실행됩니다. FastAPI는 `scripts/start.sh`가 `alembic upgrade head`를 실행한 뒤 uvicorn을 띄우고, Spring은 Flyway가 애플리케이션 기동 중에 실행하고 Hibernate가 스키마를 검증합니다(`ddl-auto=validate`).

블루그린 배포 중에는 이전 버전과 새 버전이 같은 DB를 함께 사용합니다. 따라서 마이그레이션은 이전 버전이 계속 동작할 수 있는 변경(expand)만 포함합니다. 컬럼 추가는 `NOT NULL DEFAULT` 또는 nullable로 하고, 컬럼 삭제·이름 변경은 이전 버전이 모두 내려간 다음 릴리즈(contract)에서 합니다.

FastAPI는 `scripts/start.sh migrate`로 마이그레이션만 실행할 수 있습니다. 플랫폼이 Cloud Run Job이나 ECS task로 마이그레이션을 분리하게 되면 이 명령을 사용합니다.

## 릴리즈 시나리오

| 버전 | 변경 | 시연 |
| --- | --- | --- |
| `1.0.0` | notes 생성·조회·삭제, 파란 배너, 마이그레이션 V1 (`notes` 테이블) | 최초 배포 |
| `1.1.0` | `done` 컬럼 추가 (V2, `NOT NULL DEFAULT false`), 완료 체크 UI, 초록 배너 | 블루그린·카나리 전환, 마이그레이션 |
| `1.2.0` | 코드 변경 없음. `APP_FORCE_UNHEALTHY=true`로 배포 | 자동 롤백 |

이미지 태그는 git commit SHA를 사용합니다. 버전 값은 빌드 인자로 넣습니다.

```bash
# notes-fastapi 또는 notes-spring 에서
docker build --platform linux/amd64 \
  --build-arg APP_VERSION=1.0.0 \
  --build-arg GIT_SHA="$(git rev-parse --short HEAD)" \
  --build-arg BUILT_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  -t "<registry>/oneport/notes-py:$(git rev-parse --short HEAD)" .
```

## 로컬 실행

각 디렉터리의 `compose.yaml`이 PostgreSQL과 앱을 함께 띄웁니다. 테스트는 Testcontainers로 PostgreSQL을 실행하므로 Docker가 필요합니다.

```bash
# FastAPI
cd notes-fastapi
uv sync && uv run pytest
docker compose up --build

# Spring Boot
cd notes-spring
./gradlew test
docker compose up --build
```
