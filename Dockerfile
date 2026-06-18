FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
ARG MODULE
RUN mvn -pl src/${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/src/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
