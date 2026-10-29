FROM gradle:8.10.1-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
