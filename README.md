# NexEvent Backend

Backend cho nền tảng bán vé sự kiện: xác thực người dùng, quản lý sự kiện, đặt vé/thanh toán, check-in QR và dashboard theo dõi realtime.

## Tech Stack

- Language: Java 17
- Framework: Spring Boot 4 (Web MVC, Security, Data JPA)
- Authentication: JWT (OAuth2 Resource Server)
- Database: PostgreSQL + Flyway
- Cache/Message support: Redis
- API docs: SpringDoc OpenAPI (Swagger UI)
- Media: Cloudinary
- Email: Spring Mail + Thymeleaf template
- Build tool: Maven Wrapper (`mvnw`, `mvnw.cmd`)

## Yêu Cầu Môi Trường

- Java JDK 17+
- Docker Desktop (để chạy Redis local)
- Maven (không bắt buộc nếu dùng Maven Wrapper)

## Biến môi trường

Khuyến nghị tách toàn bộ secret ra biến môi trường (Không hardcode trong `application.properties`).

Tối thiểu cần có:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_BASE64_SECRET`
- `JWT_ACCESS_TOKEN_VALIDITY_IN_SECONDS`
- `JWT_REFRESH_TOKEN_VALIDITY_IN_SECONDS`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

Bạn có thể map các biến này trong `application.properties` hoặc tạo file riêng (`application-dev.properties`).

## Cách chạy local

1) Clone project:

```bash
git clone <repo-url>
cd nexevent
```

2) Chạy Redis bằng Docker Compose:

```bash
docker compose up -d
```

3) Cấu hình biến môi trường cho PostgreSQL, JWT, Mail, Cloudinary.

4) Chạy app:

- Windows (PowerShell/CMD):

```bash
.\mvnw.cmd spring-boot:run
```

- macOS/Linux:

```bash
./mvnw spring-boot:run
```

5) Truy cập Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

## Các lệnh thường dùng

- Chạy unit/integration test:

```bash
./mvnw test
```

- Build jar:

```bash
./mvnw clean package
```

- Chạy native build (nếu cần):

```bash
./mvnw spring-boot:build-image -Pnative
./mvnw native:compile -Pnative
./mvnw test -PnativeTest
```

## Cấu trúc theo thư mục chính

```text
src/main/java/com/nexevent/nexevent
|- configs/       # Cấu hình Security, OpenAPI, Cloudinary...
|- controllers/   # REST APIs (auth, event, order, ticket, checkin...)
|- services/      # Business logic
|- repositories/  # Data access layer (JPA)
|- domains/
|  |- entities/   # Entity model
|  |- dto/        # Request/Response DTO
|  |- enums/      # Enum nghiệp vụ
|- tasks/         # Scheduled/background tasks
|- utils/         # Helper classes
```

## API Chính

- Auth: đăng ký, đăng nhập, refresh token, quên/reset mật khẩu
- Event: CRUD sự kiện, tìm kiếm/lọc
- Ticket/TicketType: quản lý loại vé và vé
- Order: tạo, thanh toán, hủy đơn
- Check-in: quét QR và ghi nhận vào cổng
- Dashboard: thống kê/check-in realtime

## Ghi chú bảo mật

- Không commit secret thật (DB password, JWT secret, mail app password, Cloudinary secret).
- Nếu secret đã lộ trong git history, cần rotate ngay.
- Nên dùng `.env`/secret manager cho môi trường dev, staging, production.
