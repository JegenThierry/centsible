FROM gradle:8.14-jdk21-jammy AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY budget-planner-api/build.gradle.kts ./budget-planner-api/
COPY budget-planner-jooq/build.gradle.kts ./budget-planner-jooq/
COPY budget-planner-core/build.gradle.kts ./budget-planner-core/
COPY budget-planner-rest/build.gradle.kts ./budget-planner-rest/
COPY budget-planner-proto/build.gradle.kts ./budget-planner-proto/
COPY budget-planner-export/build.gradle.kts ./budget-planner-export/

RUN gradle :budget-planner-export:dependencies --no-daemon --quiet || true

COPY budget-planner-api/src ./budget-planner-api/src
COPY budget-planner-jooq/src/main ./budget-planner-jooq/src/main
COPY budget-planner-jooq/src/generated ./budget-planner-jooq/src/generated
COPY budget-planner-proto/src ./budget-planner-proto/src
COPY budget-planner-export/src ./budget-planner-export/src

RUN gradle :budget-planner-export:bootJar -x jooqCodegen --no-daemon --quiet

FROM eclipse-temurin:21-jre-jammy AS release
WORKDIR /app

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY --from=builder /app/budget-planner-export/build/libs/*.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --destination /tmp/extracted \
 && CP="$(find /tmp/extracted -name '*.jar' | tr '\n' ':')" \
 && java -cp "$CP" com.microsoft.playwright.CLI install --with-deps chromium \
 && rm -rf /tmp/extracted \
 && groupadd -r spring && useradd -r -g spring spring \
 && chown -R spring:spring /app /ms-playwright

USER spring:spring
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
