# Multi-stage Dockerfile for auctionServiceServer
FROM gradle:8.6-jdk21 AS builder

WORKDIR /home/gradle/project
# Copy the project files
COPY . .
# Build the project
RUN chmod +x ./gradlew && \
    ./gradlew :auctionServiceServer:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre

# Install netcat for healthcheck
RUN apt-get update && apt-get install -y netcat-traditional && rm -rf /var/lib/apt/lists/*

COPY --from=builder /home/gradle/project/auctionServiceServer/build/libs/*.jar /app/app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app/app.jar"]
