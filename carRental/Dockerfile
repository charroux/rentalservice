# Multi-stage Dockerfile for carRental service
# Builder stage: use the official Gradle image with JDK 21
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project
# Copy the whole repo for multi-module build
COPY . .
# Build the Spring Boot jar from root project
RUN chmod +x ./gradlew && \
    ./gradlew :carRental:bootJar -x test --no-daemon

# Runtime stage: small JRE image
FROM eclipse-temurin:21-jre
ARG JAR_FILE=build/libs/*.jar
# Copy the built jar from the builder stage
COPY --from=builder /home/gradle/project/carRental/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
