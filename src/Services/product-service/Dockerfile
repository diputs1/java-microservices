FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src/common ./src/common
COPY src/product-service ./src/product-service

RUN mvn -pl src/product-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/src/product-service/target/*.jar app.jar

EXPOSE 5002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
