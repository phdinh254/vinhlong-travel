# VinhLong Tour — đặt tour và gợi ý lịch trình du lịch tỉnh Vĩnh Long

Đồ án môn **Lập trình ứng dụng hướng đối tượng**: website giới thiệu du lịch, đặt tour và gợi ý lịch trình cho khách du lịch tỉnh Vĩnh Long mới (khu vực Vĩnh Long, Bến Tre, Trà Vinh cũ).

> Backend đã viết lại xong trên nhánh `hoanglong` (10 giai đoạn, xem [`docs/KE_HOACH_BACKEND.md`](docs/KE_HOACH_BACKEND.md)).
> **Frontend vẫn gọi API của bản cũ** nên chưa chạy cùng backend mới; việc cần sửa nằm ở [`docs/API_CHANGES.md`](docs/API_CHANGES.md).

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security + JWT, Actuator), Flyway, springdoc-openapi (Swagger) |
| CSDL | PostgreSQL 17 chạy bằng Docker — **không dùng XAMPP/MySQL** |
| Frontend | React 19 + Vite, React Router, Axios |
| Kiểm thử | JUnit 5, Testcontainers (PostgreSQL thật trong Docker), Postman |

## Chạy nhanh (máy mới clone về)

Cần cài sẵn **JDK 21** (ví dụ `winget install EclipseAdoptium.Temurin.21.JDK`, kiểm tra `java -version` ra `21`; máy còn JDK cũ thì đặt `JAVA_HOME` trỏ tới JDK 21) và **Docker Desktop** (đang mở). Không cần cài Maven.

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows CMD/PowerShell dùng `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"` cho lệnh cuối.

Khoảng 30 giây sau backend chạy ở http://localhost:8081. Bảng trong CSDL do **Flyway** tự tạo, không cần import file SQL. Profile `dev` tự nạp **dữ liệu demo** khi CSDL chưa có tour nào:

| | |
|---|---|
| Admin | `admin@vinhlongtour.vn` / `Admin@12345` |
| Khách mẫu | `khach@demo.vn` / `Demo@12345` (có sẵn một đơn đã thanh toán) |
| Dữ liệu | 38 điểm đến thật, 4 tour đang bán với 12 đợt khởi hành trong tháng tới, chính sách hủy, điểm đón, phụ thu, 3 đơn ở 3 trạng thái, bài viết, cấu hình trang, một tin nhắn liên hệ |

| Địa chỉ | Nội dung |
|---|---|
| http://localhost:8081/swagger-ui.html | Tài liệu và thử mọi API. Gọi `POST /api/v1/auth/login`, chép `token`, bấm **Authorize** và dán vào để thử API cần đăng nhập |
| http://localhost:8081/actuator/health | Tình trạng backend (`UP`) |

Frontend (khi đã sửa theo API mới): `cd frontend && cp .env.example .env && npm install && npm run dev` → http://localhost:5173.

Dừng CSDL: `docker compose down` (giữ dữ liệu) hoặc `docker compose down -v` (xóa sạch, lần chạy sau nạp lại demo). Nếu cổng 5432 đã bị chiếm: `DB_PORT=5433 docker compose up -d` và đặt `DB_URL=jdbc:postgresql://localhost:5433/vinhlongtour`.

## Chạy không có dữ liệu demo (giống môi trường thật)

Bỏ `-Dspring-boot.run.profiles=dev` và tự đặt biến môi trường. Profile `dev` có mật khẩu, khóa viết sẵn trong `application-dev.yml` nên **không bao giờ bật khi chạy thật**.

```bash
ADMIN_PASSWORD=MatKhauAdminCuaBan JWT_SECRET=chuoi-ngau-nhien-it-nhat-32-ky-tu ./mvnw spring-boot:run
# Windows PowerShell: $env:ADMIN_PASSWORD="..."; $env:JWT_SECRET="..."; .\mvnw.cmd spring-boot:run
```

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `admin@vinhlongtour.vn`, *trống* | Tài khoản admin đầu tiên. Chỉ tạo khi CSDL chưa có admin; để trống mật khẩu thì không tạo |
| `JWT_SECRET` | *trống* | Khóa ký token đăng nhập, ít nhất 32 ký tự. Để trống: sinh khóa ngẫu nhiên mỗi lần chạy, phải đăng nhập lại sau khi khởi động lại backend |
| `GOOGLE_CLIENT_ID` | *trống* | Client ID (loại Web application) trên Google Cloud Console, giống `VITE_GOOGLE_CLIENT_ID` của frontend. Để trống thì đăng nhập Google báo `GOOGLE_NOT_CONFIGURED` |
| `CORS_ORIGINS` | `http://localhost:5173` | Địa chỉ frontend được gọi API, nhiều địa chỉ cách nhau bằng dấu phẩy |
| `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` | *trống* | Mã website và khóa bí mật VNPay (đăng ký sandbox tại https://sandbox.vnpayment.vn/devreg/). Để trống thì tạo link thanh toán báo `VNPAY_NOT_CONFIGURED`. Profile `dev` dùng khóa giả: tạo link được nhưng trang VNPay sẽ báo sai chữ ký, muốn thanh toán thử thật thì đặt khóa sandbox của nhóm |
| `VNPAY_RETURN_URL` | `http://localhost:5173/thanh-toan/ket-qua` | Trang frontend VNPay đưa khách về; trang này gọi `GET /api/v1/payments/vnpay/return` kèm nguyên chuỗi tham số VNPay |
| `VNPAY_PROCESS_ON_RETURN` | `true` | Ghi nhận kết quả ngay ở trang kết quả (dữ liệu có chữ ký nên tin được). Cần khi chạy localhost vì VNPay không gọi IPN vào máy cá nhân được. Khi có tên miền thật, khai báo IPN URL `https://<backend>/api/v1/payments/vnpay/ipn` trên cổng VNPay |
| `UPLOAD_DIR` | `uploads` (trong thư mục `backend`) | Nơi lưu ảnh admin tải lên, phục vụ ở `http://localhost:8081/uploads/<tên file>`. Chỉ nhận JPEG, PNG, GIF, WebP, tối đa 5 MB |
| `SERVER_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD` | khớp `docker-compose.yml` | Cổng backend và kết nối CSDL |

## Kiểm thử

```bash
cd backend
./mvnw verify
```

Cần Docker Desktop đang chạy: test tích hợp tự bật một PostgreSQL tạm trong Docker và xóa đi sau khi xong. Có test riêng cho từng giai đoạn, gồm: hai người tranh chỗ cuối chỉ một người được, IPN VNPay gửi hai lần chỉ ghi một lần, chữ ký VNPay khớp bản cài độc lập bằng Python, ví dụ tính tay của thuật toán gợi ý lịch trình ra đúng từng số, dashboard đếm đúng.

### Kiểm thử API bằng Postman

Bộ request có kiểm tra tự động nằm ở `backend/postman/VinhLong Tour API/` (định dạng thư mục YAML của Postman 12, mỗi giai đoạn một thư mục). Biến `adminPassword`, `vnpHashSecret` có sẵn giá trị khớp profile `dev`.

1. Mở Postman 12, mở thư mục dự án này (Postman đọc `.postman/resources.yaml` và tự hiện collection **VinhLong Tour API**).
2. Bật backend (profile `dev`), bấm chuột phải vào collection → **Run** để chạy toàn bộ theo thứ tự.

Chạy bằng dòng lệnh (Postman CLI, không cần đăng nhập Postman), từ thư mục gốc dự án:

```bash
npx -p postman-cli postman collection run "backend/postman/VinhLong Tour API"
```

Chạy backend với mật khẩu/khóa khác thì thêm `--env-var "adminPassword=..." --env-var "vnpHashSecret=..."`.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [`docs/KE_HOACH_BACKEND.md`](docs/KE_HOACH_BACKEND.md) | Kế hoạch, kiến trúc, OOP áp dụng ở đâu, thuật toán gợi ý lịch trình (TOPTW + Iterated Local Search) và bảng so sánh với cách tham lam |
| [`docs/API_CHANGES.md`](docs/API_CHANGES.md) | Đối chiếu API cũ → mới theo từng trang frontend |
| [`docs/diagrams/class-diagram.drawio.png`](docs/diagrams/class-diagram.drawio.png) | Sơ đồ lớp các lớp chính (mở file `.drawio` hoặc chính file `.png` bằng draw.io để sửa) |
| [`docs/diagrams/erd.drawio.png`](docs/diagrams/erd.drawio.png) | ERD 37 bảng, sinh từ các file migration |
| [`docs/data/`](docs/data/) | 38 điểm đến thật (CSV), nguồn và độ tin cậy từng dòng |

## Cấu trúc thư mục

```
vinhlong-travel/
├── docker-compose.yml        # PostgreSQL cho môi trường phát triển
├── backend/
│   ├── src/main/java/vn/edu/tvu/vinhlongtour/
│   │   ├── auth/             # tài khoản, đăng nhập, JWT, Google
│   │   ├── catalog/          # khu vực, loại hình, dịch vụ, loại khách, điểm đón, phụ thu, chính sách hủy, điểm đến
│   │   ├── tour/             # tour, lịch trình, đợt khởi hành
│   │   ├── booking/          # đặt tour, giữ chỗ, tính giá
│   │   ├── payment/          # VNPay, ghi nhận thanh toán thủ công
│   │   ├── cancellation/     # hủy tour, hoàn tiền, chuyển đợt
│   │   ├── planner/          # gợi ý lịch trình (thuật toán ILS) và lịch trình đã lưu
│   │   ├── content/          # tin tức, cấu hình trang, liên hệ
│   │   ├── upload/           # upload ảnh
│   │   ├── dashboard/        # số liệu trang quản trị
│   │   ├── demo/             # dữ liệu demo (chỉ profile dev)
│   │   └── common/           # lỗi chung, phân trang, cấu hình
│   ├── src/main/resources/db/migration/   # Flyway: V1__... tới V9__...
│   └── postman/              # bộ kiểm thử Postman + file mẫu để thử upload
├── frontend/                 # React (đang chờ sửa theo API mới)
├── docs/                     # kế hoạch, đối chiếu API, sơ đồ, dữ liệu điểm đến
└── .claude/skills/           # skill dùng chung cho Claude Code
```

Các gói lớn (`auth`, `catalog`, `tour`, `booking`, `planner`, `content`) chia tiếp `domain` (entity tự giữ quy tắc), `repository`, `service`, `web` (controller + DTO); gói nhỏ để phẳng.

## Quy ước viết code

- Tên class, method, biến, bảng, API bằng **tiếng Anh**.
- Mỗi class có **chú thích Javadoc tiếng Việt ở đầu**, nói class đó là gì và dùng để làm gì.
- Entity không có setter tùy tiện: muốn đổi dữ liệu thì gọi phương thức nghiệp vụ (`departure.reserveSeats(n)`, `booking.recordManualPayment(...)`), để quy tắc nằm một chỗ.
- Lỗi trả về cho frontend luôn có dạng `{ "code": "...", "message": "...", "fields": {...} }`.
