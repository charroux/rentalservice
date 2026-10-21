FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY rentalQueryService/build.gradle ./rentalQueryService/
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew
RUN ./gradlew :rentalQueryService:dependencies --no-daemon
COPY rentalQueryService/ ./rentalQueryService/
RUN ./gradlew :rentalQueryService:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
RUN addgroup --system spring && adduser --system spring --ingroup spring && \
    apt-get update --allow-releaseinfo-change && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /home/gradle/project/rentalQueryService/build/libs/*.jar app.jar
USER spring:spring
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
