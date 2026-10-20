FROM gradle:8.10.1-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY rental-producer/build.gradle rental-producer/build.gradle
COPY insurance-consumer/build.gradle insurance-consumer/build.gradle
COPY insurance-consumer/src insurance-consumer/src
RUN gradle :insurance-consumer:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/insurance-consumer/build/libs/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
