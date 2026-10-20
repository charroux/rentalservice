FROM gradle:8.10.1-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY rental-producer/build.gradle rental-producer/build.gradle
COPY insurance-consumer/build.gradle insurance-consumer/build.gradle
COPY rental-producer/src rental-producer/src
RUN gradle :rental-producer:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/rental-producer/build/libs/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
