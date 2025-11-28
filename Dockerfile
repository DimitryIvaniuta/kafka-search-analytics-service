# ---- Build stage -------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Pre-download dependencies & build fat jar
RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

# ---- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Copy built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
