# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Build product-service and cache downloaded Maven libraries between builds.
RUN --mount=type=bind,source=.,target=/workspace,rw \
    --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl src/Services/product-service -am -DskipTests clean package && \
    cp src/Services/product-service/target/*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=10001:10001 /tmp/app.jar /app/app.jar

EXPOSE 5002
USER 10001:10001
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
