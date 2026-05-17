FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY centsible-api/build.gradle.kts ./centsible-api/
COPY centsible-jooq/build.gradle.kts ./centsible-jooq/
COPY centsible-core/build.gradle.kts ./centsible-core/
COPY centsible-proto/build.gradle.kts ./centsible-proto/
COPY centsible-export/build.gradle.kts ./centsible-export/
COPY centsible-rest/build.gradle.kts ./centsible-rest/
# Integration provider modules. Each plugin lives in its own subdir.
COPY centsible-integrations ./centsible-integrations

RUN gradle dependencies --no-daemon --quiet || true

COPY centsible-api/src ./centsible-api/src
COPY centsible-jooq/src/main ./centsible-jooq/src/main
COPY centsible-jooq/src/generated ./centsible-jooq/src/generated
COPY centsible-proto/src ./centsible-proto/src
COPY centsible-core/src ./centsible-core/src
COPY centsible-rest/src ./centsible-rest/src
RUN gradle :centsible-rest:bootJar -x jooqCodegen --no-daemon --quiet

FROM eclipse-temurin:21-jre-alpine AS release
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --chown=spring:spring --from=builder /app/centsible-rest/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
