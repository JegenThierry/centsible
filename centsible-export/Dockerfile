FROM gradle:8.14-jdk21-jammy AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY centsible-api/build.gradle.kts ./centsible-api/
COPY centsible-jooq/build.gradle.kts ./centsible-jooq/
COPY centsible-core/build.gradle.kts ./centsible-core/
COPY centsible-rest/build.gradle.kts ./centsible-rest/
COPY centsible-proto/build.gradle.kts ./centsible-proto/
COPY centsible-export/build.gradle.kts ./centsible-export/
COPY centsible-imports/core/build.gradle.kts ./centsible-imports/core/
COPY centsible-imports/csv/build.gradle.kts ./centsible-imports/csv/
COPY centsible-imports/ofx/build.gradle.kts ./centsible-imports/ofx/

RUN gradle :centsible-export:dependencies --no-daemon --quiet || true

COPY centsible-api/src ./centsible-api/src
COPY centsible-jooq/src/main ./centsible-jooq/src/main
COPY centsible-jooq/src/generated ./centsible-jooq/src/generated
COPY centsible-proto/src ./centsible-proto/src
COPY centsible-export/src ./centsible-export/src

# Materialise the runtime config from the committed example; secrets/overrides come from env vars at runtime.
RUN cp centsible-export/src/main/resources/application.properties.example \
       centsible-export/src/main/resources/application.properties

RUN gradle :centsible-export:bootJar -x jooqCodegen --no-daemon --quiet

FROM eclipse-temurin:21-jre-jammy AS release
WORKDIR /app

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
# Only Chromium is launched by the app; skip the runtime auto-download that would also fetch Firefox/WebKit.
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

COPY --from=builder /app/centsible-export/build/libs/*.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --destination /tmp/extracted \
 && CP="$(find /tmp/extracted -name '*.jar' | tr '\n' ':')" \
 && java -cp "$CP" com.microsoft.playwright.CLI install --with-deps chromium \
 && rm -rf /tmp/extracted \
 && groupadd -r spring && useradd -r -g spring spring \
 && chown -R spring:spring /app /ms-playwright

USER spring:spring
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
