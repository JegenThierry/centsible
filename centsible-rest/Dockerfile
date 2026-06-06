# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
ENV GRADLE_USER_HOME=/root/.gradle

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY centsible-api/build.gradle.kts ./centsible-api/
COPY centsible-jooq/build.gradle.kts ./centsible-jooq/
COPY centsible-core/build.gradle.kts ./centsible-core/
COPY centsible-proto/build.gradle.kts ./centsible-proto/
COPY centsible-export/build.gradle.kts ./centsible-export/
COPY centsible-rest/build.gradle.kts ./centsible-rest/
COPY centsible-imports/core/build.gradle.kts ./centsible-imports/core/
COPY centsible-imports/csv/build.gradle.kts ./centsible-imports/csv/
COPY centsible-imports/ofx/build.gradle.kts ./centsible-imports/ofx/
COPY centsible-integrations ./centsible-integrations

RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew :centsible-rest:dependencies --no-daemon --quiet || true

COPY centsible-api/src ./centsible-api/src
COPY centsible-jooq/src/main ./centsible-jooq/src/main
COPY centsible-jooq/src/generated ./centsible-jooq/src/generated
COPY centsible-proto/src ./centsible-proto/src
COPY centsible-imports/core/src ./centsible-imports/core/src
COPY centsible-imports/csv/src ./centsible-imports/csv/src
COPY centsible-imports/ofx/src ./centsible-imports/ofx/src
COPY centsible-core/src ./centsible-core/src
COPY centsible-rest/src ./centsible-rest/src
RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew :centsible-rest:bootJar -x jooqCodegen --no-daemon

FROM eclipse-temurin:21-jre-alpine AS release
WORKDIR /app

LABEL org.opencontainers.image.title="centsible-rest" \
      org.opencontainers.image.description="Centsible REST API (Spring Boot)"

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --chown=spring:spring --from=builder /app/centsible-rest/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
