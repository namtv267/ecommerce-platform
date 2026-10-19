# Dùng chung cho mọi service:  docker build --build-arg SERVICE=order-service .
FROM gradle:8.10.2-jdk21 AS build
ARG SERVICE
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY libs ./libs
COPY services ./services
RUN gradle :services:${SERVICE}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
RUN useradd -r -u 1001 app && mkdir -p /var/log/ecommerce && chmod 777 /var/log/ecommerce
COPY --from=build /workspace/services/${SERVICE}/build/libs/*.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
