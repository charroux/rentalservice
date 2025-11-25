# Multi-stage Dockerfile for auctionServiceServer - Optimized
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project

# Optimisation : copier d'abord les fichiers de build pour utiliser le cache Docker
COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY auctionService/build.gradle ./auctionService/
COPY auctionServiceServer/build.gradle ./auctionServiceServer/

# Télécharger les dépendances (mise en cache Docker)
RUN chmod +x ./gradlew && ./gradlew :auctionServiceServer:dependencies --no-daemon || true

# Copier le code source (auctionServiceServer dépend de auctionService)
COPY auctionService/ ./auctionService/
COPY auctionServiceServer/ ./auctionServiceServer/

# Build optimisé
RUN ./gradlew :auctionServiceServer:bootJar -x test --no-daemon --parallel

# Runtime stage: image JRE optimisée
FROM eclipse-temurin:21-jre-jammy

# Créer un utilisateur non-root pour la sécurité
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Installer les outils nécessaires (netcat pour healthcheck + curl)
RUN apt-get update --allow-releaseinfo-change || apt-get update && \
    apt-get install -y --no-install-recommends netcat-traditional curl && \
    rm -rf /var/lib/apt/lists/*

# Configuration optimisée
WORKDIR /app
COPY --from=builder /home/gradle/project/auctionServiceServer/build/libs/*.jar app.jar

# Changer vers utilisateur non-root
USER spring:spring

# JVM optimisée pour containers + gRPC
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Dio.netty.tryReflectionSetAccessible=true"

EXPOSE 9090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
