# Multi-stage Docker build for Fairshare Spring Boot backend using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN printf "nameserver 8.8.8.8\nnameserver 1.1.1.1\n" > /etc/resolv.conf && mvn dependency:go-offline -B

# Copy sources and build package
COPY src ./src
RUN printf "nameserver 8.8.8.8\nnameserver 1.1.1.1\n" > /etc/resolv.conf && mvn clean package -DskipTests -B

# Extract Spring Boot layers for fast Docker startup and caching
RUN java -Djarmode=layertools -jar target/fairshare-backend-0.0.1-SNAPSHOT.jar extract

# Production lightweight JRE runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication -Xss256k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-${SERVER_PORT:-8080}} org.springframework.boot.loader.launch.JarLauncher"]
