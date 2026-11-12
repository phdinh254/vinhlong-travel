# Hệ thống bán tour du lịch tỉnh Vĩnh Long (mới)

Đồ án: website bán tour du lịch trên địa bàn tỉnh Vĩnh Long sau hợp nhất (khu vực Vĩnh Long, Bến Tre, Trà Vinh cũ).

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation), JWT (jjwt 0.12), google-api-client |
| Frontend | React 19 + Vite, React Router 6, Axios, @react-oauth/google |
| CSDL | MySQL 8.0+ / MariaDB 10.4+ (chạy bằng XAMPP) |
| Tích hợp | Đăng nhập Google (Google Identity Services), thanh toán VNPay (API v2.1.0) |

## 1. Cấu trúc thư mục

```
vinhlong-tour/
├── database/
│   ├── 01_schema.sql      # Tạo CSDL VinhLongTour: 39 giải pháp Q01–Q39 + bảng hệ thống/CMS
│   └── 02_seed.sql        # Danh mục + dữ liệu MẪU (tour, đợt, giá) để chạy thử
├── backend/               # Spring Boot (package vn.edu.tvu.vinhlongtour)
│   └── src/main/java/.../
│       ├── entity/        # 32 entity ánh xạ đúng tên bảng/cột trong SQL
│       ├── repository/    # Spring Data JPA
│       ├── service/       # Nghiệp vụ: đặt tour, tính giá, thanh toán VNPay, hủy/hoàn/chuyển đợt
│       ├── controller/    # API công khai, khách hàng, admin
│       ├── security/      # JWT filter
│       └── config/        # Security, CORS, khởi tạo admin, dữ liệu mẫu (H2)
└── frontend/              # React
    └── src/
        ├── layouts/       # PublicLayout (5 mục menu) + AdminLayout
        ├── pages/public/  # Trang chủ, Giới thiệu, Dịch vụ, Tin tức, Liên hệ, đặt tour, tra cứu đơn...
        └── pages/admin/   # Tổng quan, Tour & đợt, Đơn đặt, Yêu cầu hủy, Danh mục, Tin tức, Liên hệ...
```

## 2. Chạy dự án

### 2.1. Cơ sở dữ liệu (MySQL trong XAMPP)
1. Mở **XAMPP Control Panel** → bấm **Start** dòng **MySQL** (không cần Apache cho backend).
2. Vào `http://localhost/phpmyadmin` → tab **SQL** (hoặc **Import**) → chạy `database/01_schema.sql`. Script tự tạo database `vinhlongtour` (utf8mb4).
3. Chạy tiếp `database/02_seed.sql` để có danh mục và dữ liệu mẫu.
   Dòng lệnh tương đương: `E:\XAMPP\mysql\bin\mysql -u root --default-character-set=utf8mb4 < database\01_schema.sql` (tương tự với 02).
   > Tour, đợt khởi hành, giá trong seed là **dữ liệu mẫu**, cần thay bằng số liệu thật.

### 2.2. Backend
Yêu cầu JDK 17+ và Maven 3.9+.

```bash
cd backend
# Cấu hình bằng biến môi trường (hoặc sửa src/main/resources/application.yml)
# Mặc định đã khớp XAMPP: jdbc:mysql://localhost:3306/vinhlongtour, user root, mật khẩu rỗng.
# Chỉ cần đặt lại nếu bạn đã đổi mật khẩu root:
set DB_PASSWORD=MatKhauCuaBan
set JWT_SECRET=chuoi-bi-mat-dai-toi-thieu-32-ky-tu
set GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
set VNPAY_TMN_CODE=...
set VNPAY_HASH_SECRET=...
mvn spring-boot:run
```

Lần chạy đầu, backend tự tạo tài khoản admin: **admin@vinhlongtour.vn / Admin@123** (đổi bằng `ADMIN_EMAIL`, `ADMIN_PASSWORD`). Hãy đổi mật khẩu sau khi đăng nhập.

**Chạy thử không cần MySQL** (CSDL H2 trong bộ nhớ, tự sinh dữ liệu mẫu):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev-h2
```

### 2.3. Frontend
Yêu cầu Node.js 20+.
```bash
cd frontend
cp .env.example .env      # điền VITE_API_URL (mặc định http://localhost:8081), VITE_GOOGLE_CLIENT_ID
npm install
npm run dev               # http://localhost:5173
```

### 2.4. Cấu hình đăng nhập Google
1. Google Cloud Console → APIs & Services → Credentials → **Create OAuth client ID** → loại *Web application*.
2. *Authorized JavaScript origins*: thêm `http://localhost:5173` (và domain thật khi triển khai).
3. Dùng cùng Client ID cho `VITE_GOOGLE_CLIENT_ID` (frontend) và `GOOGLE_CLIENT_ID` (backend).

Luồng: nút Google trả về **ID token** → frontend gửi `POST /api/auth/google` → backend kiểm tra chữ ký, issuer, audience bằng `GoogleIdTokenVerifier` → tìm/tạo `TaiKhoan` (liên kết theo `GoogleSub` hoặc email) và `KhachHang` → cấp JWT của hệ thống.

### 2.5. Cấu hình VNPay (sandbox)
1. Đăng ký tài khoản kiểm thử tại `https://sandbox.vnpayment.vn/devreg/`, nhận `vnp_TmnCode` và `vnp_HashSecret` qua email.
2. `VNPAY_RETURN_URL` = `http://localhost:5173/thanh-toan/ket-qua` (trang React nhận kết quả).
3. **IPN URL** (khai báo với VNPay) = `https://<domain-backend>/api/payments/vnpay/ipn`. URL này phải truy cập được từ Internet.
4. Khi chạy localhost, VNPay không gọi được IPN; vì vậy `VNPAY_PROCESS_ON_RETURN=true` cho phép ghi nhận kết quả ở bước return (đã kiểm tra chữ ký). **Khi triển khai thật phải đặt `false`** và chỉ tin IPN.
5. Dùng thẻ test do VNPay cung cấp trong tài liệu sandbox (`https://sandbox.vnpayment.vn/apis/`).

Cài đặt chữ ký (xem `VnPayService`): sắp xếp tham số `vnp_*` theo tên, URL-encode giá trị, nối bằng `&`, ký HMAC-SHA512 với `HashSecret`; `vnp_Amount` = số tiền × 100. Xử lý IPN có tính **idempotent** (một `vnp_TxnRef` chỉ ghi nhận thành công một lần), kiểm tra số tiền khớp, trả `RspCode` 00/01/02/04/97/99.

## 3. Người dùng và chức năng

| Nhóm | Chức năng |
|---|---|
| **Khách vãng lai** | Xem 5 mục; tìm/lọc tour; đặt tour không cần tài khoản; thanh toán VNPay; tra cứu đơn bằng *mã đơn + SĐT*; gửi yêu cầu hủy; gửi liên hệ |
| **Khách hàng** | Như trên + đăng ký/đăng nhập (email hoặc Google), quản lý hồ sơ, xem lịch sử đơn |
| **Admin** | Tổng quan; quản lý tour (ảnh, dịch vụ, lịch trình mẫu), đợt khởi hành (giá theo loại khách, điểm đón, lịch trình riêng); đơn đặt (xác nhận thanh toán thủ công kèm chứng từ, hủy, hoàn thành); duyệt yêu cầu hủy; hoàn tiền nhiều lần; chuyển đợt; danh mục; chính sách hủy có phiên bản; Giới thiệu/thông tin liên hệ; Tin tức; Liên hệ; khóa/mở tài khoản |

**5 mục quản trị nội dung website:** Trang chủ (tour nổi bật, tin mới – lấy từ dữ liệu tour/tin tức), Giới thiệu (`CauHinhTrang.GIOI_THIEU`), Dịch vụ (danh sách tour), Tin tức (`TinTuc`), Liên hệ (`LienHe` + thông tin trong `CauHinhTrang`).

## 4. Ánh xạ 39 giải pháp thiết kế → mã nguồn

| Mã | Bảng (SQL) | Hiện thực trong backend |
|---|---|---|
| Q01 | `Tour` | `Tour`, `AdminTourService.saveTour` |
| Q02 | `LoaiHinh`, `Tour_LoaiHinh` | `@ManyToMany loaiHinhs`, lọc `loaiHinhId` |
| Q03 | `KhuVuc`, `Tour_KhuVuc` | `@ManyToMany khuVucs`, lọc `khuVucId` |
| Q04–Q05 | `DiemDen` | Không nối N-N với Tour; nối qua `HoatDongLichTrinh.DiemDenID` |
| Q06 | `HinhAnhTour` (`LaAnhDaiDien`, `ThuTuHienThi`) | Đảm bảo luôn có đúng 1 ảnh đại diện |
| Q07 | `DichVu`, `Tour_DichVu.LoaiApDung` | Hiển thị Bao gồm/Không bao gồm, lọc tour theo dịch vụ bao gồm |
| Q08–Q10 | `NgayLichTrinh`, `HoatDongLichTrinh` | `ItineraryEditor` (frontend) |
| Q11–Q12 | `HoatDongLichTrinh.DiemDenID` NULL được | Một điểm đến xuất hiện nhiều lần |
| Q13 | `NgayLichTrinh.DotKhoiHanhID` (CHECK: thuộc Tour **hoặc** Đợt) | Sao chép lịch trình mẫu khi tạo đợt; `PUT /api/admin/dots/{id}/lich-trinh` |
| Q14–Q15 | `DotKhoiHanh` | `AdminTourService.saveDot` |
| Q16–Q17 | `LoaiKhach`, `BangGiaDot` | `PricingService.timGia` |
| Q18 | `LoaiKhach.TuoiTu/TuoiDen` | Tuổi tính đến **ngày khởi hành** (`Period.between`) |
| Q19 | `LoaiKhach.HeSoChiemCho` | Số chỗ chiếm = tổng hệ số |
| Q20 | `DiemDon`, `DotKhoiHanh_DiemDon` (`GioDon`, `PhiPhuThuDon`) | Phí đón tính theo hành khách (`HanhKhach.PhiDon`) |
| Q21 | `PhuThu.CachTinh`, `ChiTietPhuThuDon` | THEO_NGUOI / THEO_DON / THEO_SO_LUONG |
| Q22–Q23 | `TaiKhoan`, `KhachHang.TaiKhoanID` NULL được | Khách vãng lai dùng lại hồ sơ theo SĐT |
| Q24, Q26 | `KhachHang` ≠ `HanhKhach` | Người đặt khác người đi |
| Q25 | `DonDatTour.DotKhoiHanhID` | 1 đơn = 1 đợt |
| Q27–Q28 | `HanhKhach` (`GiaApDung` chốt giá) | Đổi giá đợt không ảnh hưởng đơn cũ |
| Q29 | `DonDatTour` | Mã đơn `VLyyMMdd` + 5 ký tự ngẫu nhiên |
| Q30 | `LichSuTrangThaiDon` | `BookingSupport.doiTrangThai` ghi mọi lần đổi |
| Q31 | `DonDatTour.HanGiuCho` | Khóa dòng đợt (`PESSIMISTIC_WRITE`) khi giữ chỗ; `BookingExpiryJob` mỗi phút trả chỗ đơn quá hạn |
| Q32–Q34 | `ThanhToan` | Cọc/tất toán nhiều lần; VNPay + xác nhận thủ công kèm ảnh chứng từ |
| Q35–Q36 | `YeuCauHuy`, `ChiTietHuyHanhKhach` | Hủy toàn bộ / từng hành khách, tính phí phạt từng người |
| Q37 | `ChinhSachHuy`, `MocPhatHuy`, `DonDatTour.ChinhSachHuyID` | Chốt phiên bản chính sách khi đặt |
| Q38 | `HoanTien` | Hoàn nhiều lần, trừ vào `DaThanhToan` (thu ròng) |
| Q39 | `LichSuChuyenDot` | Tính lại giá theo đợt mới, ghi chênh lệch |

Bảng bổ sung ngoài 39 giải pháp: `VaiTro`, `CauHinhTrang`, `TinTuc`, `LienHe`; cột `DonDatTour.TongTienVe/TongPhuThu/DaThanhToan/SoChoChiem` để tính toán nhanh; `ThanhToan.MaThamChieu` (= `vnp_TxnRef`, UNIQUE) và `DuLieuPhanHoi` (log phản hồi cổng).

## 5. Danh sách API chính

| Nhóm | Phương thức & đường dẫn |
|---|---|
| Xác thực | `POST /api/auth/register`, `/login`, `/google` · `GET/PUT /api/me` · `PUT /api/me/mat-khau` · `GET /api/me/bookings` |
| Công khai | `GET /api/public/tours?q&loaiHinhId&khuVucId&dichVuId&noiBat&page` · `/tours/{id}` · `/dots/{id}` · `/dots/{id}/lich-trinh` · `/danh-muc` · `/cau-hinh` · `/tin-tuc` · `/tin-tuc/{slug}` · `POST /lien-he` |
| Đặt tour | `POST /api/bookings/bao-gia` · `POST /api/bookings` · `GET /api/bookings/{maDon}?sdt=` · `POST /api/bookings/{maDon}/yeu-cau-huy` |
| Thanh toán | `POST /api/payments/vnpay/create` · `GET /api/payments/vnpay/return` · `GET /api/payments/vnpay/ipn` |
| Admin | `/api/admin/tours`, `/dots`, `/dots/{id}/lich-trinh`, `/bookings` (+ `/trang-thai`, `/thanh-toan`, `/hoan-tien`, `/chuyen-dot`), `/yeu-cau-huy`, `/danh-muc/{loai-hinh|khu-vuc|dich-vu|loai-khach|diem-den|diem-don|phu-thu|chinh-sach-huy}`, `/tin-tuc`, `/lien-he`, `/cau-hinh`, `/users`, `/files`, `/dashboard` |

## 6. Quy tắc nghiệp vụ đáng chú ý
- **Chống bán vượt chỗ:** mọi thao tác thay đổi `SoChoDaDat` đều khóa dòng `DotKhoiHanh` trong transaction; CHECK `SoChoDaDat <= SucChuaToiDa` ở CSDL là lớp bảo vệ cuối.
- Đơn phải có ít nhất 1 hành khách từ 18 tuổi; hủy từng phần không được để lại đơn toàn trẻ em.
- Cọc tối thiểu = `deposit-percent` (mặc định 30%) tổng tiền, làm tròn lên hàng nghìn.
- Thanh toán VNPay về sau khi đơn đã hết hạn: hệ thống thử giữ lại chỗ; nếu đợt đã hết chỗ, ghi lịch sử "CẦN HOÀN TIỀN" để admin xử lý.
- Phí phạt hủy từng phần được giữ lại và cộng vào `TongPhuThu`; tiền cần hoàn = đã thu − tổng mới.

## 7. Hướng phát triển
Gửi email xác nhận/nhắc thanh toán; hoàn tiền tự động qua API hoàn tiền VNPay; trình soạn thảo WYSIWYG cho tin tức; báo cáo doanh thu theo tháng/tour; kiểm thử tự động (JUnit + Testcontainers MySQL).

> Lưu ý: dự án KHÔNG chạy qua Apache/PHP của XAMPP. XAMPP chỉ cung cấp MySQL (và phpMyAdmin để quản lý). Backend chạy bằng Java (cổng 8081), frontend chạy bằng Node (cổng 5173). Thư mục dự án đặt ở đâu cũng được, kể cả trong `htdocs`.
