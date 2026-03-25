# Этап сборки (build stage)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Сборка приложения без тестов (чтобы не тратить время)
RUN mvn clean package -DskipTests

# Этап выполнения (runtime stage)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Копируем jar из сборщика
COPY --from=builder /app/target/assess-pro-0.0.1-SNAPSHOT.jar app.jar
# Порт приложения
EXPOSE 8080
# Точка входа: активируем профиль prod и передаём переменные окружения
ENTRYPOINT ["java", "-jar", "app.jar"]