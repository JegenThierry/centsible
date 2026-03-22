FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle.kts ./

RUN gradle dependencies --no-daemon --quiet || true

COPY src ./src
RUN gradle bootJar -x jooqCodegen --no-daemon --quiet

FROM eclipse-temurin:21-jre-alpine AS release
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --chown=spring:spring --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
