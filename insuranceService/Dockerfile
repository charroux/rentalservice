FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project

COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY insuranceService/build.gradle ./insuranceService/
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew
RUN ./gradlew :insuranceService:dependencies --no-daemon

COPY insuranceService/ ./insuranceService/
RUN ./gradlew :insuranceService:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN apt-get update --allow-releaseinfo-change || apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /home/gradle/project/insuranceService/build/libs/*.jar app.jar
USER spring:spring
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
