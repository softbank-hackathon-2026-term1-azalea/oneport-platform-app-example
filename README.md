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
| `GET /notes?limit=50` | `[{"id","title","created_at","done"}]` 최신순 (`limit` 1~200) | DB 읽기 |
| `POST /notes` | 본문 `{"title"}` → `201` 생성된 노트. 공백·200자 초과는 `400`(Spring) / `422`(FastAPI) | DB 쓰기 |
| `PATCH /notes/{id}` | 본문 `{"done": true}` → `200` 변경된 노트. 없는 id는 `404` | DB 쓰기 (1.1.0부터) |
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
| `GIT_SHA` | 이미지 빌드 시 `--build-arg`로 지정 | `/version`에 노출됩니다. 지정하지 않으면 `unknown` |
| `APP_COLOR` | CSS 색상 | 배너 색. 버전별 기본값을 이미지에 두고 환경변수로 바꿀 수 있습니다 |
| `APP_FORCE_UNHEALTHY` | `true`/`false` | `true`면 `/health`가 503을 반환합니다 |
| `LOG_FORMAT` | `json`(기본) 또는 `text` | 플랫폼에서는 `json`, 로컬에서는 `text` |
| `LOG_LEVEL` | 기본 `INFO` | |

## 로그

두 앱 모두 stdout에 한 줄 JSON으로 기록합니다. 모든 로그에 `request_id`가 들어가고, FastAPI는 `app`·`version` 필드를, Spring은 ECS 형식의 `service.name`·`service.version` 필드를 함께 기록합니다. 요청 로그는 `method`, `path`, `status`, `duration_ms`를 포함하며 `/health`, `/ready`는 기록하지 않습니다.

## DB 마이그레이션

마이그레이션은 컨테이너 시작 시 서버보다 먼저 실행됩니다. FastAPI는 `scripts/start.sh`가 DB 연결을 기다린 뒤 Alembic 마이그레이션을 적용하고 uvicorn을 띄웁니다. Spring은 Flyway가 애플리케이션 기동 중에 마이그레이션을 적용하고, Hibernate가 스키마를 검증합니다(`ddl-auto=validate`).

블루그린·카나리 배포 중에는 이전 버전과 새 버전이 같은 DB를 함께 사용합니다. 따라서 마이그레이션은 이전 버전이 계속 동작할 수 있는 변경(expand)만 포함합니다. 컬럼 추가는 `NOT NULL DEFAULT` 또는 nullable로 하고, 컬럼 삭제·이름 변경은 이전 버전이 모두 내려간 다음 릴리즈(contract)에서 합니다.

DB 스키마가 실행 중인 릴리즈보다 새 버전이면 두 앱 모두 마이그레이션을 건너뛰고 기동합니다. FastAPI는 `database schema is newer than this release` 경고를 남기고, Spring은 Flyway가 미래 버전 마이그레이션을 무시합니다. 이 동작 덕분에 스키마를 되돌리지 않고 이전 버전으로 롤백할 수 있습니다.

FastAPI는 `scripts/start.sh migrate`로 마이그레이션만 실행할 수 있습니다. 플랫폼이 Cloud Run Job이나 ECS task로 마이그레이션을 분리하게 되면 이 명령을 사용합니다.

## 릴리즈와 태그

릴리즈마다 git 태그 `v<버전>`을 붙입니다. 두 앱은 같은 태그를 공유하고, 화면과 `/version`에 표시되는 버전은 코드(FastAPI `pyproject.toml`, Spring `build.gradle.kts`)에서 읽습니다.

| 태그 | 브랜치 | 스키마 | 내용 | 용도 |
| --- | --- | --- | --- | --- |
| `v1.0.0` | `main` | V1 | notes 생성·조회·삭제, 파란 배너 | 최초 릴리즈 기록용. 시연에는 `v1.0.1`을 사용합니다 |
| `v1.0.1` | `release/1.0` | V1 | `v1.0.0` + 빌드 시각 기록, 새 스키마에서 기동하는 롤백 대응 | 최초 배포, 롤백 대상 |
| `v1.1.0` | `main` | V2 | `done` 컬럼(`NOT NULL DEFAULT false`), `PATCH /notes/{id}`, 완료 체크 UI, 초록 배너 | 기능 릴리즈 기록용. 시연에는 `v1.1.1`을 사용합니다 |
| `v1.1.1` | `main` | V2 | `v1.1.0` + 롤백 대응 | 블루그린·카나리 전환, 마이그레이션, 자동 롤백 |

`v1.0.0`, `v1.1.0`의 FastAPI 이미지는 DB 스키마가 더 새 버전이면 기동하지 않습니다. 롤백이 포함된 시연에는 `v1.0.1`, `v1.1.1`을 사용합니다.

이미지는 태그를 체크아웃한 상태에서 빌드하고, 이미지 태그에는 커밋 SHA를 사용합니다. `GIT_SHA`를 넘기면 화면과 `/version`에 표시됩니다.

```bash
git checkout v1.1.1
cd notes-fastapi   # 또는 notes-spring
docker build --platform linux/amd64 \
  --build-arg GIT_SHA="$(git rev-parse --short HEAD)" \
  -t "<registry>/oneport/notes-py:$(git rev-parse --short HEAD)" .
```

## 시연 절차

각 단계에서 브라우저로 `https://<app-id>.app.yubin.dev`를 열어 두면 상단 배너가 1초마다 `/version`을 조회해 버전, 색상, 인스턴스 이름을 갱신합니다. 같은 내용을 명령줄에서 확인할 수도 있습니다.

```bash
watch -n 1 'curl -s https://notes-py.app.yubin.dev/version'
```

### 1. 최초 배포 (`v1.0.1`)

`v1.0.1` 이미지를 배포합니다. 컨테이너가 시작되면서 V1 마이그레이션(`notes` 테이블)이 적용됩니다.

- 배너가 파란색이고 버전이 `1.0.1`로 표시됩니다.
- 노트를 몇 개 추가해 둡니다. 다음 단계에서 기존 데이터가 유지되는지 확인하는 데 사용합니다.
- 로그에서 FastAPI는 `migrations applied`, Spring은 `Successfully applied 1 migration`을 확인할 수 있습니다.

### 2. 블루그린 배포 (`v1.0.1` → `v1.1.1`)

`v1.1.1` 이미지를 `BLUE_GREEN` 전략으로 배포합니다.

1. green task가 시작되면서 V2 마이그레이션(`done` 컬럼 추가)이 적용됩니다. 이 시점에 blue(`1.0.1`)는 계속 요청을 처리하며, 추가된 컬럼과 무관하게 정상 동작합니다.
2. green이 health check를 통과하면 트래픽이 green으로 전환되고 배너가 초록색, 버전이 `1.1.1`로 바뀝니다.
3. 1단계에서 추가한 노트가 그대로 보이고, 모두 완료되지 않은 상태(`done: false`)입니다. 체크박스로 완료 상태를 바꿀 수 있습니다.
4. bake time이 지나면 blue task가 종료됩니다.

### 3. 카나리 배포

1단계 상태에서 `v1.1.1`을 `CANARY` 전략으로 배포하면 일부 요청만 새 버전으로 전달됩니다. 새로고침할 때마다 배너가 파란색(`1.0.1`)과 초록색(`1.1.1`) 사이에서 바뀌는 것으로 비율을 확인할 수 있습니다. `1.0.1`이 응답한 화면에는 완료 체크박스가 표시되지 않습니다.

### 4. 자동 롤백 (health check 실패)

`v1.1.1` 이미지를 환경변수 `APP_FORCE_UNHEALTHY=true`로 다시 배포합니다. 코드 변경 없이 배포 설정만 바꾼 릴리즈입니다.

1. 새 task의 `/health`가 `503 {"status":"unhealthy","reason":"APP_FORCE_UNHEALTHY"}`를 반환합니다.
2. 새 task가 health check를 통과하지 못하므로 트래픽은 기존 task에 머물거나, 전환 직후 플랫폼의 롤백 설정(배포 알람 또는 배포 실패 처리)에 따라 이전 task로 돌아갑니다.
3. 배너와 `/version`은 계속 이전 버전을 표시합니다. `/ready`는 DB가 정상이므로 `200`입니다.

### 5. 수동 롤백 (`v1.1.1` → `v1.0.1`)

배포가 완료된 뒤 이전 버전으로 되돌릴 때는 `v1.0.1` 이미지를 다시 배포합니다. DB 스키마는 V2로 유지됩니다.

- `v1.0.1`은 스키마가 더 새 버전인 것을 감지하고 마이그레이션을 건너뛴 채 기동합니다.
- 배너가 파란색, 버전이 `1.0.1`로 돌아옵니다. `done` 컬럼은 남아 있지만 `1.0.1`은 사용하지 않습니다.
- 노트 추가·조회·삭제는 정상 동작합니다. `1.0.1`이 추가한 노트는 DB 기본값에 따라 `done: false`가 됩니다.

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
