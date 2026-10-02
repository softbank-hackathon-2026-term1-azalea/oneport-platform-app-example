# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies --quiet

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar -x test --quiet
RUN java -Djarmode=tools -jar build/libs/app.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre

ARG APP_VERSION=dev
ARG GIT_SHA=unknown
ARG BUILT_AT=unknown
ENV APP_VERSION=${APP_VERSION} GIT_SHA=${GIT_SHA} BUILT_AT=${BUILT_AT} PORT=8080

RUN groupadd --system --gid 999 app && useradd --system --gid 999 --uid 999 --create-home app
WORKDIR /app

COPY --from=builder --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/application/ ./

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
