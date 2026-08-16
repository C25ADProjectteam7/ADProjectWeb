# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk update && apk upgrade --no-cache libcrypto3 libssl3
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8081/api/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50.0", "-XX:InitialRAMPercentage=25.0", "-jar", "app.jar"]
