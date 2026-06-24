# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

RUN --mount=type=bind,source=.,target=/workspace,rw \
    --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl src/background-job-service -am -DskipTests clean package && \
    cp src/background-job-service/target/*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

COPY --from=build --chown=10001:10001 /tmp/app.jar /app/app.jar

EXPOSE 5007
USER 10001:10001
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
