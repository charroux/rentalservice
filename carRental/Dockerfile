# Multi-stage Dockerfile for carRental service - Optimized
# Builder stage: use the official Gradle image with JDK 21
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project

# Optimisation : copier d'abord les fichiers de build pour utiliser le cache Docker
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY carRental/build.gradle ./carRental/

# Télécharger les dépendances (mise en cache Docker)
RUN chmod +x ./gradlew && ./gradlew :carRental:dependencies --no-daemon || true

# Copier le code source
COPY carRental/ ./carRental/

# Build optimisé
RUN ./gradlew :carRental:bootJar -x test --no-daemon --parallel

# Runtime stage: image JRE optimisée
FROM eclipse-temurin:21-jre-jammy

# Créer un utilisateur non-root pour la sécurité
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Installer seulement les outils nécessaires
RUN apt-get update --allow-releaseinfo-change || apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Configuration optimisée
WORKDIR /app
COPY --from=builder /home/gradle/project/carRental/build/libs/*.jar app.jar

# Changer vers utilisateur non-root
USER spring:spring

# JVM optimisée pour containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
