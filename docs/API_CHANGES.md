# Đối chiếu API cũ → API mới (cho dev frontend)

Backend đã viết lại hoàn toàn (nhánh `hoanglong`). Frontend hiện vẫn gọi API cũ (`/api/public/...`, `/api/admin/...`, tên trường tiếng Việt) nên **chưa chạy cùng backend mới**. Tài liệu này liệt kê việc cần sửa theo từng trang.

- **Chi tiết từng trường, ví dụ request/response**: mở Swagger `http://localhost:8081/swagger-ui.html` khi backend đang chạy. Swagger luôn đúng với code; tài liệu này chỉ chỉ đường.
- **Thử nhanh**: bộ Postman `backend/postman/VinhLong Tour API/` có request mẫu chạy được cho mọi chức năng.
- **Dữ liệu để thử giao diện**: chạy backend với profile `dev` (xem README) sẽ có sẵn 4 tour, đợt khởi hành, đơn mẫu, bài viết, cấu hình trang. Admin `admin@vinhlongtour.vn` / `Admin@12345`, khách `khach@demo.vn` / `Demo@12345`.

## 1. Quy tắc chung đã đổi

| | Cũ | Mới |
|---|---|---|
| Tiền tố | `/api/public/...`, `/api/admin/...`, `/api/...` | `/api/v1/...` (công khai), `/api/v1/admin/...` (chỉ ADMIN), `/api/v1/me/...` (khách đã đăng nhập) |
| Tên trường JSON | Tiếng Việt: `tenTour`, `maDon`, `tinTucId`, `trangThai` | Tiếng Anh camelCase: `name`, `code`, `id`, `status` |
| Khóa chính | `tourId`, `dotKhoiHanhId`, `donDatTourId`... | Luôn là `id`; tham chiếu sang bảng khác là `tourId`, `departureId`... |
| Danh sách có phân trang | Spring Page (`content`, `totalPages`...) | `{ "items": [...], "page": 0, "size": 20, "total": 57 }` — `page` đếm từ 0, số trang = `ceil(total / size)` |
| Lỗi | Không thống nhất | Luôn `{ "code": "SEAT_NOT_ENOUGH", "message": "câu tiếng Việt", "fields": { "email": "..." } }`. Hiện `message` cho người dùng (hàm `errMsg` hiện có vẫn dùng được); dùng `code` khi cần xử lý riêng; `fields` có khi `code = VALIDATION_FAILED` để tô đỏ từng ô |
| Đăng nhập | Token trong `Authorization: Bearer` | Giữ nguyên. Token sống 2 giờ; hết hạn → 401 `UNAUTHORIZED` (interceptor 401 hiện có vẫn đúng). Tài khoản bị khóa bị chặn ngay ở request kế tiếp (403 `ACCOUNT_LOCKED`) |
| Ngày giờ | Lẫn lộn | Chuỗi ISO giờ Việt Nam không kèm múi giờ: `2026-11-19T07:00:00`; ngày: `2026-11-19`; giờ: `07:30` |
| Tiền | Số | Số nguyên đồng (không có phần lẻ) |
| Ảnh | Upload ở `/api/admin/files` | Upload ở `POST /api/v1/admin/uploads` (multipart, trường `file`) → `{ "url": "/uploads/<tên>.png" }`. Các trường ảnh nhận `http(s)://...`, `/uploads/...` hoặc `/images/...` (ảnh có sẵn trong `frontend/public/images`). Hàm `imgUrl` hiện có đã xử lý đúng ba loại này |

## 2. Đối chiếu theo trang

### Đăng nhập, tài khoản (`auth.jsx`, `pages/public/Account.jsx`, `pages/admin/Users.jsx`)

| Cũ | Mới | Ghi chú |
|---|---|---|
| `POST /api/auth/login` | `POST /api/v1/auth/login` | Trả `{ token, expiresAt, account }` |
| `POST /api/auth/register` | `POST /api/v1/auth/register` | `{ email, password, fullName, phone }` |
| `POST /api/auth/google` | `POST /api/v1/auth/google` | `{ credential }` (ID token từ Google Identity Services); token sai → `INVALID_GOOGLE_TOKEN` |
| — | `GET /api/v1/me` | Thông tin tài khoản đang đăng nhập |
| `PUT /api/me` | `PUT /api/v1/me` | `{ fullName, phone }` |
| `PUT /api/me/mat-khau` | `PUT /api/v1/me/password` | `{ currentPassword, newPassword }` (tài khoản Google chưa có mật khẩu thì bỏ `currentPassword`) |
| `GET /api/admin/users` | `GET /api/v1/admin/accounts?q=&page=&size=` | Có phân trang |
| `PUT /api/admin/users/{taiKhoanId}/trang-thai` | `PATCH /api/v1/admin/accounts/{id}/status` | `{ status: "ACTIVE" \| "LOCKED" }`; admin không tự khóa mình được |

### Trang chủ, danh sách tour, chi tiết tour (`Home.jsx`, `Tours.jsx`, `TourDetail.jsx`, `Services.jsx`)

| Cũ | Mới | Ghi chú |
|---|---|---|
| `GET /api/public/danh-muc` (gộp mọi danh mục) | Tách riêng: `GET /api/v1/regions`, `/api/v1/tour-categories`, `/api/v1/amenities`, `/api/v1/passenger-types`, `/api/v1/surcharges`, `/api/v1/cancellation-policies/current` | Gọi song song bằng `Promise.all` |
| `GET /api/public/tours` | `GET /api/v1/tours?q=&regionId=&categoryId=&amenityId=&destinationId=&minDays=&maxDays=&featured=&page=&size=` | Mỗi thẻ có sẵn `fromPrice` (giá người lớn thấp nhất của đợt còn chỗ) và `nextDepartureAt`; `null` nghĩa là chưa có đợt đặt được |
| `GET /api/public/tours/{id}` | `GET /api/v1/tours/{id}` | Kèm ảnh, dịch vụ gồm/không gồm, phụ thu, lịch trình mẫu |
| — | `GET /api/v1/tours/{id}/departures` | Các đợt khách còn xem được; `bookable` cho biết có đặt được không, `seatsLeft` số chỗ còn |
| `GET /api/public/dots/{id}` | `GET /api/v1/departures/{id}` | Kèm bảng giá theo loại khách và điểm đón |
| `GET /api/public/dots/{id}/lich-trinh` | `GET /api/v1/departures/{id}/itinerary` | Mỗi hoạt động có thể gắn `destination` (điểm đến thật) |
| — | `GET /api/v1/destinations`, `/api/v1/destinations/{id}` | 38 điểm đến thật có tọa độ, giờ mở cửa, giá vé, độ tin cậy dữ liệu — có thể làm trang "Điểm đến" |

### Đặt tour, xem đơn, thanh toán (`Booking.jsx`, `BookingView.jsx`, `Lookup.jsx`, `PaymentResult.jsx`)

| Cũ | Mới | Ghi chú |
|---|---|---|
| `POST /api/bookings/bao-gia` | `POST /api/v1/bookings/quote` | Không cần đăng nhập. Gửi `departureId`, danh sách hành khách (`fullName`, `birthDate`, `pickupPointId`...), phụ thu đã chọn; trả giá từng người theo **tuổi tại ngày đi**, tổng tiền, tiền cọc tối thiểu |
| `POST /api/bookings` | `POST /api/v1/bookings` | Thêm `contactName`, `contactPhone`, `contactEmail`, `note`. Đăng nhập rồi thì đơn tự gắn vào tài khoản. Lỗi hay gặp: `SEAT_NOT_ENOUGH`, `ADULT_REQUIRED`, `DEPARTURE_NOT_OPEN` |
| `GET /api/bookings/{maDon}` | `GET /api/v1/bookings/lookup?code=&phone=` (khách vãng lai) hoặc `GET /api/v1/me/bookings/{code}` (đã đăng nhập) | Tra cứu luôn cần **mã đơn + số điện thoại** |
| `GET /api/me/bookings` | `GET /api/v1/me/bookings` | |
| `POST /api/payments/vnpay/create` `{ maDon, sdt, loai }` | `POST /api/v1/payments/vnpay` `{ bookingCode, phone, type: "DEPOSIT" \| "FULL" }` (`DEPOSIT`: tiền cọc tối thiểu, chỉ khi chưa trả gì; `FULL`: toàn bộ số còn lại) | Trả `{ paymentUrl, amount, reference, expiresAt }` → chuyển trang tới `paymentUrl` như cũ |
| `GET /api/payments/vnpay/return?...` | `GET /api/v1/payments/vnpay/return?...` | Truyền nguyên chuỗi tham số VNPay. Trả `{ validSignature, success, bookingCode, amount, ... }` |
| `POST /api/bookings/{maDon}/yeu-cau-huy` | `POST /api/v1/bookings/{code}/cancellation-requests` | `{ scope: "FULL" \| "PARTIAL", passengerIds, reason, phone }`. Đơn chưa trả đồng nào được hủy ngay; đã trả thì chờ admin duyệt. Phản hồi có `penaltyPercent`, `penaltyAmount`, `estimatedRefund` để hiện cho khách |

### Quản trị tour và đợt khởi hành (`Tours.jsx`, `TourForm.jsx` trong admin)

| Cũ | Mới | Ghi chú |
|---|---|---|
| `GET /api/admin/tours` | `GET /api/v1/admin/tours?q=&status=&page=&size=` | Thấy cả tour nháp/ngừng bán |
| `GET/POST/PUT/DELETE /api/admin/tours[/{id}]` | `GET/POST/PUT/DELETE /api/v1/admin/tours[/{id}]` | Ảnh và lịch trình mẫu **tách ra 2 API riêng** (dưới). Mở bán (`status: "ON_SALE"`) cần có ảnh và lịch trình đủ số ngày, thiếu thì `TOUR_INCOMPLETE` |
| (gửi kèm trong tour) | `PUT /api/v1/admin/tours/{id}/images` | `{ images: [{ url, caption, cover }] }`, ảnh đầu tiên là ảnh đại diện nếu không đánh dấu |
| (gửi kèm trong tour) | `PUT /api/v1/admin/tours/{id}/itinerary` | `{ days: [{ title, description, activities: [{ startTime, title, description, destinationId }] }] }` |
| `POST /api/admin/dots` | `POST /api/v1/admin/tours/{id}/departures` | Đợt mới tự sao chép lịch trình mẫu của tour. Bảng giá `prices` phải đủ mọi loại khách |
| — | `GET /api/v1/admin/tours/{id}/departures` | |
| `PUT /api/admin/dots/{id}`, `DELETE ...` | `PUT /api/v1/admin/departures/{id}`, `DELETE ...` | Không xóa/hủy được đợt đã có khách (`DEPARTURE_HAS_BOOKINGS`); không giảm sức chứa dưới số chỗ đã đặt |
| `GET/PUT /api/admin/dots/{id}/lich-trinh` | `GET/PUT /api/v1/admin/departures/{id}/itinerary` | Sửa lịch trình riêng của một đợt, không ảnh hưởng tour |
| `POST /api/admin/files` | `POST /api/v1/admin/uploads` | Chỉ nhận ảnh JPEG/PNG/GIF/WebP ≤ 5 MB (`NOT_AN_IMAGE`, `FILE_TOO_LARGE`) |

### Quản trị danh mục (`Catalogs.jsx`)

| Cũ | Mới |
|---|---|
| `/api/admin/danh-muc/khu-vuc` | `/api/v1/admin/regions` (đọc: `/api/v1/regions`) |
| `/api/admin/danh-muc/loai-hinh` | `/api/v1/admin/tour-categories` |
| `/api/admin/danh-muc/dich-vu` | `/api/v1/admin/amenities` |
| `/api/admin/danh-muc/loai-khach` | `/api/v1/admin/passenger-types` — khoảng tuổi không được chồng nhau |
| `/api/admin/danh-muc/diem-don` | `/api/v1/admin/pickup-points` |
| `/api/admin/danh-muc/phu-thu` | `/api/v1/admin/surcharges` |
| `/api/admin/danh-muc/chinh-sach-huy` | `/api/v1/admin/cancellation-policies` — chính sách đã có hiệu lực thì không sửa/xóa được (đơn cũ đã chốt theo nó), muốn đổi thì tạo chính sách mới |
| `/api/admin/danh-muc/diem-den` | `/api/v1/admin/destinations` — có tọa độ, giờ mở cửa, ngày đóng cửa, giá vé, thời gian tham quan (dùng cho gợi ý lịch trình) |

Mỗi danh mục đều có `GET` (danh sách), `POST` (thêm), `PUT /{id}` (sửa), `DELETE /{id}` (xóa; đang được dùng thì `DATA_CONFLICT`).

### Quản trị đơn, hủy, hoàn tiền (`Bookings.jsx`, `BookingDetail.jsx`, `CancelRequests.jsx`)

| Cũ | Mới | Ghi chú |
|---|---|---|
| `GET /api/admin/bookings` | `GET /api/v1/admin/bookings?q=&status=&tourId=&departureId=&needsRefund=&page=&size=` | `needsRefund=true`: đơn đã thu thừa cần hoàn |
| `GET /api/admin/bookings/{id}` | `GET /api/v1/admin/bookings/{id}` | Kèm hành khách, phụ thu, thanh toán, yêu cầu hủy, hoàn tiền, chuyển đợt, lịch sử trạng thái, `refundDue`, và `moneyHistory` (sổ tiền: thanh toán thành công và hoàn tiền theo thời gian, `amount` dương là tiền vào, âm là tiền ra) |
| `PUT /api/admin/bookings/{id}/trang-thai` | `PATCH /api/v1/admin/bookings/{id}/status` | Chỉ đổi tay sang `COMPLETED` (sau chuyến đi) hoặc `CANCELLED` (đơn chưa thu tiền); các trạng thái khác tự đổi theo tiền |
| `POST /api/admin/bookings/{id}/thanh-toan` | `POST /api/v1/admin/bookings/{id}/payments` | Ghi nhận tiền mặt/chuyển khoản `{ amount, method: "CASH" \| "BANK_TRANSFER", transactionNo, proofUrl, note }` |
| `POST /api/admin/bookings/{id}/hoan-tien` | `POST /api/v1/admin/bookings/{id}/refunds` | Hoàn nhiều lần được, không vượt `refundDue` (`REFUND_EXCEEDS_DUE`) |
| `POST /api/admin/bookings/{id}/chuyen-dot` | `POST /api/v1/admin/bookings/{id}/transfer` | `{ departureId, reason }`; chỉ sang đợt của cùng tour, giá tính lại theo tuổi ở ngày đi mới |
| `GET /api/admin/yeu-cau-huy` | `GET /api/v1/admin/cancellation-requests?status=PENDING` | |
| `PUT /api/admin/yeu-cau-huy/{id}` | `POST /api/v1/admin/cancellation-requests/{id}/approve` hoặc `/reject` | Body tùy chọn `{ note }` |

### Tin tức, liên hệ, cấu hình trang, dashboard

| Cũ | Mới | Ghi chú |
|---|---|---|
| `GET /api/public/tin-tuc?chuyenMuc=` | `GET /api/v1/articles?category=&page=&size=` | Chỉ bài đã xuất bản, không kèm nội dung |
| `GET /api/public/tin-tuc/{slug}` | `GET /api/v1/articles/{slug}` | Mỗi lần gọi tăng `viewCount` |
| `GET/POST/PUT/DELETE /api/admin/tin-tuc[/{id}]` | `GET/POST/PUT/DELETE /api/v1/admin/articles[/{id}]` | `slug` để trống thì tự tạo từ tiêu đề; sửa bài không đổi slug cũ |
| `POST /api/public/lien-he` | `POST /api/v1/contact-messages` | `{ fullName, phone, email, subject, message }`, cần SĐT hoặc email. Các ô phụ của form (tour, ngày đi, số người, kênh liên hệ) ghép vào `message` như frontend đang làm |
| `GET /api/admin/lien-he` | `GET /api/v1/admin/contact-messages?status=NEW` | |
| `PUT /api/admin/lien-he/{id}` `{ trangThai }` | `PATCH /api/v1/admin/contact-messages/{id}/status` `{ status }` | |
| `GET /api/public/cau-hinh` | `GET /api/v1/site-settings` | Luôn đủ 13 khóa (chưa nhập là chuỗi rỗng) |
| `GET/PUT /api/admin/cau-hinh` | `PUT /api/v1/admin/site-settings` | Chỉ gửi các khóa cần đổi. Khóa dạng link phải bắt đầu bằng `http(s)://` (`INVALID_LINK`) |
| `GET /api/admin/dashboard` | `GET /api/v1/admin/dashboard?from=&to=` | Mặc định từ đầu tháng tới hôm nay. Xem bảng trường ở mục 3 |

### Chức năng mới, chưa có trang

| API | Dùng cho |
|---|---|
| `GET /api/v1/itineraries/lodging-presets` | Danh sách nơi lưu trú gợi ý sẵn (có tọa độ) |
| `POST /api/v1/itineraries/suggest` | **Trang "Gợi ý lịch trình"**: khách chọn ngày đi, số ngày (1–5), nhịp đi (`RELAXED`/`MODERATE`/`PACKED`), nơi lưu trú (`lodging` hoặc `lodgingLatitude` + `lodgingLongitude`), số người, ngân sách, sở thích (`categoryIds`), khu vực (`regionIds`). Trả lịch từng ngày (giờ đến, giờ bắt đầu, giờ rời, thời gian đi đường, bữa trưa), chi phí ước tính và tối đa 3 tour đang bán giống nhất để đặt luôn. Mỗi điểm có `hoursQuality`/`priceQuality` để ghi chú "ước tính" |
| `POST /api/v1/itineraries`, `GET /api/v1/me/itineraries[/{id}]`, `DELETE /api/v1/me/itineraries/{id}` | Khách đăng nhập lưu và xem lại lịch trình |
| `GET /api/v1/destinations` | Trang/bản đồ điểm đến |

## 3. Giá trị trạng thái và tên khóa

| Nhóm | Cũ → Mới |
|---|---|
| Trạng thái đơn | `CHO_THANH_TOAN` → `PENDING_PAYMENT`, `DA_COC` → `DEPOSITED`, `DA_THANH_TOAN` → `PAID`, `HOAN_THANH` → `COMPLETED`, `DA_HUY` → `CANCELLED`, `HET_HAN` → `EXPIRED` |
| Trạng thái tour | `NHAP` → `DRAFT`, `DANG_BAN` → `ON_SALE`, `NGUNG_BAN` → `STOPPED` |
| Trạng thái đợt | `MO_BAN` → `OPEN`, `DONG` → `CLOSED`, `HUY` → `CANCELLED`. Không còn `HET_CHO` và `DA_KHOI_HANH`: dùng `seatsLeft == 0` và `bookable` để hiện "Hết chỗ"/"Đã đóng" |
| Cách tính phụ thu | `THEO_NGUOI` → `PER_PERSON`, `THEO_DON` → `PER_BOOKING`, `THEO_SO_LUONG` → `PER_UNIT` |
| Loại hoạt động lịch trình | Bỏ (`LOAI_HOAT_DONG`); hoạt động chỉ còn giờ, tiêu đề, mô tả và điểm đến gắn kèm |
| Thanh toán | Phương thức `VNPAY`, `CASH`, `BANK_TRANSFER`; trạng thái `PENDING`, `SUCCEEDED`, `FAILED` |
| Yêu cầu hủy | Phạm vi `FULL`/`PARTIAL`; trạng thái `PENDING`, `APPROVED`, `REJECTED` |
| Chuyên mục bài viết | `TIN_TUC` → `NEWS`, `VAN_HOA` → `CULTURE`, `SU_KIEN` → `EVENT`, `KINH_NGHIEM` → `TIPS` |
| Trạng thái bài viết | `NHAP` → `DRAFT`, `XUAT_BAN` → `PUBLISHED` |
| Tin nhắn liên hệ | `MOI` → `NEW`, `DA_XU_LY` → `HANDLED` |
| Khóa cấu hình trang | `GIOI_THIEU` → `ABOUT_HTML`, `HOTLINE`, `EMAIL`, `DIA_CHI` → `ADDRESS`, `BAN_DO_NHUNG` → `MAP_EMBED_URL`, `GIO_LAM_VIEC` → `SUPPORT_HOURS`, `FACEBOOK` → `FACEBOOK_URL`, `INSTAGRAM` → `INSTAGRAM_URL`, `YOUTUBE` → `YOUTUBE_URL`, `TIKTOK` → `TIKTOK_URL`, `CS_DIEU_KHOAN` → `TERMS_HTML`, `CS_BAO_MAT` → `PRIVACY_HTML`, `CS_HUY_TOUR` → `CANCELLATION_HTML` |
| Dashboard | `tongDaThu` → `collected` (và mới: `refunded`, `netRevenue` = thu − hoàn), `tongDon` → `newBookings`, `donChoThanhToan`/`donDaCoc`/`donDaThanhToan` → `bookingsByStatus.PENDING_PAYMENT`/`.DEPOSITED`/`.PAID`, `yeuCauHuyCho` → `pendingCancellationRequests`, `lienHeMoi` → `newContactMessages`, `tongTour` → `toursOnSale`, `donGanDay` → `recentBookings`. Mới: `bookingsNeedingRefund` + `bookingsNeedingRefundList` |

## 4. Lưu ý an toàn cho frontend

- Nội dung bài viết và các khóa `*_HTML` là HTML do admin soạn, đang được hiển thị bằng `dangerouslySetInnerHTML`. Nên lọc qua thư viện như DOMPurify trước khi hiển thị.
- Không lưu mật khẩu hay token ở chỗ khác ngoài `localStorage` như hiện tại; khi nhận 401 thì xóa token và chuyển về trang đăng nhập (đã có trong `api.js`).
