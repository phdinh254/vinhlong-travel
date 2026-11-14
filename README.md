# VinhLong Tour — đặt tour và gợi ý lịch trình du lịch tỉnh Vĩnh Long

Đồ án môn **Lập trình ứng dụng hướng đối tượng**: website giới thiệu du lịch, đặt tour và gợi ý lịch trình cho khách du lịch tỉnh Vĩnh Long mới (khu vực Vĩnh Long, Bến Tre, Trà Vinh cũ).

> **Đang xây dựng lại backend** trên nhánh `hoanglong`, theo kế hoạch ở [`docs/KE_HOACH_BACKEND.md`](docs/KE_HOACH_BACKEND.md).
> Hiện xong **giai đoạn 3** (tài khoản, đăng nhập; danh mục và 38 điểm đến; tour, lịch trình, đợt khởi hành). Frontend vẫn gọi API của bản cũ nên chưa chạy cùng backend mới; bảng đối chiếu API cũ → mới sẽ có ở `docs/API_CHANGES.md` (giai đoạn 9).

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security + JWT, Actuator), Flyway, springdoc-openapi (Swagger) |
| CSDL | PostgreSQL 17 chạy bằng Docker — **không dùng XAMPP/MySQL** |
| Frontend | React 19 + Vite, React Router, Axios |
| Kiểm thử | JUnit 5, Testcontainers (PostgreSQL thật trong Docker) |

## Cấu trúc thư mục

```
vinhlong-travel/
├── docker-compose.yml   # PostgreSQL cho môi trường phát triển
├── backend/             # Spring Boot, gói vn.edu.tvu.vinhlongtour
│   └── src/main/resources/db/migration/   # Flyway: các file tạo bảng (V1__..., V2__...)
├── frontend/            # React
├── docs/
│   ├── KE_HOACH_BACKEND.md   # Kế hoạch 10 giai đoạn
│   └── data/                 # Dữ liệu 38 điểm đến thật (CSV) + nguồn
└── .claude/skills/      # Skill dùng chung cho Claude Code
```

## Cài đặt (một lần)

1. **JDK 21** — ví dụ Temurin: `winget install EclipseAdoptium.Temurin.21.JDK`. Kiểm tra: `java -version` phải ra `21`.
   Nếu máy còn JDK cũ, đặt `JAVA_HOME` trỏ tới thư mục JDK 21.
2. **Docker Desktop** — mở lên trước khi chạy CSDL hoặc chạy test.
3. **Node.js 20+** cho frontend.

Không cần cài Maven: dự án có sẵn `mvnw` / `mvnw.cmd`.

## Chạy dự án

```bash
# 1. CSDL (từ thư mục gốc dự án)
docker compose up -d

# 2. Backend — http://localhost:8081
cd backend
ADMIN_PASSWORD=MatKhauAdminCuaBan ./mvnw spring-boot:run
# Windows CMD:        set ADMIN_PASSWORD=MatKhauAdminCuaBan && mvnw.cmd spring-boot:run
# Windows PowerShell: $env:ADMIN_PASSWORD="MatKhauAdminCuaBan"; .\mvnw.cmd spring-boot:run

# 3. Frontend — http://localhost:5173
cd frontend
cp .env.example .env
npm install
npm run dev
```

Bảng trong CSDL do **Flyway** tự tạo khi backend khởi động, không cần import file SQL bằng tay.

| Địa chỉ | Nội dung |
|---|---|
| http://localhost:8081/swagger-ui.html | Tài liệu và thử API (Swagger) |
| http://localhost:8081/actuator/health | Tình trạng backend (`UP`) |

Cấu hình qua biến môi trường:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `admin@vinhlongtour.vn`, *trống* | Tài khoản admin đầu tiên. Chỉ tạo khi CSDL chưa có admin; để trống mật khẩu thì không tạo |
| `JWT_SECRET` | *trống* | Khóa ký token đăng nhập, ít nhất 32 ký tự. Để trống: sinh khóa ngẫu nhiên mỗi lần chạy, phải đăng nhập lại sau khi khởi động lại backend |
| `GOOGLE_CLIENT_ID` | *trống* | Client ID (loại Web application) trên Google Cloud Console, giống `VITE_GOOGLE_CLIENT_ID` của frontend. Để trống thì đăng nhập Google báo `GOOGLE_NOT_CONFIGURED` |
| `CORS_ORIGINS` | `http://localhost:5173` | Địa chỉ frontend được gọi API, nhiều địa chỉ cách nhau bằng dấu phẩy |
| `SERVER_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD` | khớp `docker-compose.yml` | Cổng backend và kết nối CSDL |

Nếu cổng 5432 đã bị chiếm, chạy `DB_PORT=5433 docker compose up -d` và đặt `DB_URL=jdbc:postgresql://localhost:5433/vinhlongtour`.

Thử API cần đăng nhập trên Swagger: gọi `POST /api/v1/auth/login`, chép `token` trong kết quả, bấm **Authorize** ở đầu trang và dán vào.

Dừng CSDL: `docker compose down` (giữ dữ liệu) hoặc `docker compose down -v` (xóa sạch dữ liệu).

## Kiểm thử

```bash
cd backend
./mvnw verify
```

Cần Docker Desktop đang chạy: test tích hợp tự bật một PostgreSQL tạm trong Docker và xóa đi sau khi xong.

### Kiểm thử API bằng Postman

Bộ request có sẵn kiểm tra tự động nằm ở `backend/postman/VinhLong Tour API/`, theo định dạng thư mục YAML của Postman 12 (mỗi request là một file `.request.yaml`, sửa trong Postman thì file trong repo đổi theo). Mỗi giai đoạn thêm một thư mục request mới.

1. Mở Postman 12, mở thư mục dự án này (Postman đọc `.postman/resources.yaml` và tự hiện collection **VinhLong Tour API**).
2. Ở tab **Variables** của collection, điền `adminPassword` giống `ADMIN_PASSWORD` lúc chạy backend.
3. Bật backend, rồi bấm chuột phải vào collection → **Run** để chạy toàn bộ kiểm tra theo thứ tự.

Chạy bằng dòng lệnh (Postman CLI, không cần đăng nhập Postman):

```bash
npx -p postman-cli postman collection run "backend/postman/VinhLong Tour API" --env-var "adminPassword=MatKhauAdminCuaBan"
```

## Quy ước viết code

- Tên class, method, biến, bảng, API bằng **tiếng Anh**.
- Mỗi class có **chú thích Javadoc tiếng Việt ở đầu**, nói class đó là gì và dùng để làm gì.
- Lỗi trả về cho frontend luôn có dạng `{ "code": "...", "message": "...", "fields": {...} }`.
