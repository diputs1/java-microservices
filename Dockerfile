# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

ARG MODULE_PATH

# Build source from the context while keeping downloaded Maven libraries cached.
# Changes written to the bind mount are temporary, so copy only the final JAR out.
RUN --mount=type=bind,source=.,target=/workspace,rw \
    --mount=type=cache,target=/root/.m2 \
    test -n "${MODULE_PATH}" && \
    mvn -B -ntp -pl "${MODULE_PATH}" -am -DskipTests clean package && \
    cp "${MODULE_PATH}"/target/*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

COPY --from=build --chown=10001:10001 /tmp/app.jar /app/app.jar

USER 10001:10001
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
