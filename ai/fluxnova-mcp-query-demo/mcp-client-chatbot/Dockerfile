## Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY mcp-client-chatbot/pom.xml ./pom.xml
RUN mvn dependency:go-offline

COPY mcp-client-chatbot/src ./src
RUN mvn clean package -DskipTests

## Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S fluxnova && adduser -S fluxnova -G fluxnova

USER fluxnova

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080 5005

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
