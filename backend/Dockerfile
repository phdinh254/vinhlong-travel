# Backend Spring Boot. Build: docker compose build backend (xem docker-compose.yml ở thư mục gốc)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
# Tải thư viện trước để lần build sau chỉ sửa code thì không tải lại
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Test cần Docker (Testcontainers) nên chạy riêng bằng ./mvnw verify, không chạy trong lúc build image
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh UPLOAD_DIR=/app/uploads
COPY --from=build /src/target/vinhlong-tour-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
