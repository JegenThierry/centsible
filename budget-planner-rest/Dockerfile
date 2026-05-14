FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY budget-planner-api/build.gradle.kts ./budget-planner-api/
COPY budget-planner-jooq/build.gradle.kts ./budget-planner-jooq/
COPY budget-planner-core/build.gradle.kts ./budget-planner-core/
COPY budget-planner-proto/build.gradle.kts ./budget-planner-proto/
COPY budget-planner-export/build.gradle.kts ./budget-planner-export/
COPY budget-planner-rest/build.gradle.kts ./budget-planner-rest/

RUN gradle dependencies --no-daemon --quiet || true

COPY budget-planner-api/src ./budget-planner-api/src
COPY budget-planner-jooq/src/main ./budget-planner-jooq/src/main
COPY budget-planner-jooq/src/generated ./budget-planner-jooq/src/generated
COPY budget-planner-proto/src ./budget-planner-proto/src
COPY budget-planner-core/src ./budget-planner-core/src
COPY budget-planner-rest/src ./budget-planner-rest/src
RUN gradle :budget-planner-rest:bootJar -x jooqCodegen --no-daemon --quiet

FROM eclipse-temurin:21-jre-alpine AS release
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --chown=spring:spring --from=builder /app/budget-planner-rest/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
