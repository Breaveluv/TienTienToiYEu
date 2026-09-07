# =========================================================
# STAGE 1: Build stage (Sử dụng JDK 17 & Gradle Wrapper)
# =========================================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Sao chép các file cấu hình Gradle để cache layer dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Cấp quyền thực thi cho gradlew và tải trước dependencies
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Sao chép mã nguồn và build file JAR (bỏ qua test khi đóng gói)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# =========================================================
# STAGE 2: Runtime stage (Sử dụng JRE 17 siêu nhẹ & bảo mật)
# =========================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Tạo non-root user 'spring' và thư mục data cho H2 database
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/data \
    && chown -R spring:spring /app
USER spring:spring

# Sao chép file JAR đã đóng gói từ Stage 1
COPY --from=builder /app/build/libs/TienTienYouthManagement-0.0.1-SNAPSHOT.jar app.jar

# Render tự động cấp phát biến môi trường PORT (mặc định 8080 nếu chạy local)
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng Spring Boot với cấu hình cổng động từ Render
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
