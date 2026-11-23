# Kế hoạch xây dựng lại Backend — VinhLong Tour

> Trạng thái: **CHỜ DUYỆT (bản 3 — đã có dữ liệu điểm đến thật, khách chọn nơi lưu trú)**. Chưa viết dòng code nào cho tới khi kế hoạch này được duyệt.
> Nhánh: `hoanglong`. Backend + database cũ đã xóa (vẫn còn trong lịch sử git ở nhánh `main` để tham khảo).
>
> Bản 2 cập nhật theo góp ý: tên tiếng Anh + chú thích đầu mỗi class, Java 21, tìm hiểu lại phần gợi ý lịch trình (mục 5), dừng sau mỗi giai đoạn để duyệt (mục 7).

## 1. Mục tiêu

Website đặt tour du lịch Vĩnh Long (Vĩnh Long, Bến Tre, Trà Vinh cũ) gồm 3 phần:

1. **Giới thiệu du lịch**: tour, điểm đến, tin tức, trang giới thiệu/liên hệ.
2. **Đặt tour**: chọn đợt khởi hành, tính giá theo loại khách, giữ chỗ, thanh toán VNPay hoặc thủ công, hủy/hoàn tiền/chuyển đợt.
3. **Gợi ý lịch trình** (mới): khách nhập số ngày, sở thích, khu vực, ngân sách → hệ thống xếp điểm đến thành lịch trình từng ngày, có giờ cụ thể, và gợi ý tour có sẵn gần giống để đặt.

Yêu cầu của môn **Lập trình hướng đối tượng**: code thể hiện rõ đóng gói, kế thừa, đa hình, trừu tượng — ở chỗ nghiệp vụ thật sự cần, không thêm lớp chỉ để "trông OOP".

## 2. Công nghệ

| Thành phần | Chọn | Lý do |
|---|---|---|
| Ngôn ngữ | **Java 21 (LTS)** — cả nhóm cài JDK 21 | Có `record`, `sealed`, `switch` pattern — viết OOP gọn hơn |
| Framework | Spring Boot 4.0.x (Web, Data JPA, Security, Validation, OAuth2 Resource Server) | Kiểm tra bản patch mới nhất khi khởi tạo |
| CSDL | **MySQL 8.4 chạy bằng Docker** (`docker compose up -d`) | Bỏ XAMPP. Một lệnh là có CSDL, mọi máy giống nhau. *Đã đổi sau khi xong 10 giai đoạn:* ban đầu dùng PostgreSQL 17, chuyển sang MySQL theo yêu cầu của nhóm |
| Tạo bảng | **Flyway** (`src/main/resources/db/migration/V1__...sql`) | Bảng tự tạo khi chạy backend, không import SQL bằng tay |
| Đăng nhập | JWT (HS256) qua Spring Security; Google ID token kiểm tra bằng khóa công khai (JWKS) của Google | Dùng sẵn của Spring, bỏ `jjwt` và `google-api-client` |
| Tài liệu API | springdoc-openapi → Swagger UI tại `/swagger-ui.html` | Dev frontend xem và thử API trực tiếp |
| Kiểm thử | JUnit 5 + Mockito (logic thuần) + Testcontainers MySQL (repository, luồng đặt tour) | Test chạy trên MySQL thật |
| Build | Maven (kèm `mvnw`, không cần cài Maven) | |

Chạy dự án sau khi xong: `docker compose up -d` → `./mvnw spring-boot:run` → frontend `npm run dev`.

## 3. Kiến trúc

### 3.1. Quy ước viết code

- **Tên tiếng Anh** cho class, method, biến, bảng, cột, API. Có bảng thuật ngữ Việt–Anh ở mục 4.
- **Mỗi class có chú thích Javadoc tiếng Việt ở đầu**, nói class này là gì và dùng để làm gì. Ví dụ:

```java
/**
 * Đợt khởi hành của một tour (ngày đi, ngày về, sức chứa, số chỗ đã đặt).
 * Tự kiểm soát việc giữ chỗ / trả chỗ để không bao giờ bán vượt sức chứa.
 */
@Entity
public class Departure { ... }
```

- Trong thân method chỉ chú thích **lý do** khi code không tự nói được (ví dụ: vì sao khóa dòng theo thứ tự này).

### 3.2. Chia gói theo nghiệp vụ

```
vn.edu.tvu.vinhlongtour
├── common/        lỗi chung, phân trang, cấu hình
├── auth/          tài khoản, vai trò, đăng ký/đăng nhập, Google, JWT
├── catalog/       khu vực, loại hình, dịch vụ, điểm đến, điểm đón, loại khách, phụ thu, chính sách hủy
├── tour/          tour, ảnh, lịch trình mẫu, đợt khởi hành, bảng giá
├── booking/       đơn đặt, hành khách, báo giá, giữ chỗ, hết hạn giữ chỗ
├── payment/       thanh toán VNPay + thủ công
├── cancellation/  yêu cầu hủy, hoàn tiền, chuyển đợt
├── itinerary/     gợi ý lịch trình
├── content/       tin tức, cấu hình trang, liên hệ, upload ảnh
└── admin/         dashboard, quản lý người dùng
```

Mỗi gói có `domain` (entity + enum), `repository`, `service`, `web` (controller + DTO). Controller chỉ nhận/trả dữ liệu; nghiệp vụ nằm trong service và **trong chính entity**.

### 3.3. OOP ở đâu (để báo cáo môn học)

| Nguyên lý | Áp dụng vào chỗ nào |
|---|---|
| **Đóng gói** | Entity tự giữ quy tắc. `Departure.reserveSeats(n)` tự kiểm tra còn chỗ (thiếu thì báo `SEAT_NOT_ENOUGH`); `Booking.recordManualPayment(...)`, `Booking.refund(...)` tự cộng/trừ tiền và tự đổi trạng thái. Service không sửa thẳng các trường số chỗ, số tiền. |
| **Enum có hành vi** | `BookingStatus` khai báo bước chuyển hợp lệ (`PENDING_PAYMENT → DEPOSITED → PAID → COMPLETED`, …). Chuyển sai → lỗi ngay. |
| **Đa hình (phụ thu)** | `SurchargeMethod` = `PER_PERSON`, `PER_BOOKING`, `PER_UNIT`, mỗi giá trị tự cài `quantity(...)`. |
| **Thanh toán** | *Đã đổi khi làm giai đoạn 5:* không tạo `interface PaymentGateway` vì chỉ có VNPay là cổng thật (xác nhận thủ công do admin làm, không phải cổng). Interface chỉ một cài đặt là thừa; khi thêm cổng thứ hai (MoMo...) thì tách interface từ `VnPayGateway`. `Payment` thuộc đơn, mọi khoản tiền vào đi qua `Booking` khi đã khóa dòng đơn. |
| **Đa hình (gợi ý lịch trình)** | `interface ScoringCriterion` với 3 cài đặt: `InterestMatch` (khớp sở thích), `Popularity` (độ nổi bật), `PreferredRegion` (khu vực ưu tiên). *Đã đổi khi làm giai đoạn 7:* không tạo `interface TravelTimeEstimator`; thuật toán nhận thẳng **bảng thời gian di chuyển**, nên test dùng được bảng của ví dụ tính tay, sau này đổi cách tính đường đi chỉ cần dựng bảng khác. |
| **Kế thừa (khoản tiền)** | *Thêm sau khi xong 10 giai đoạn:* lớp trừu tượng `MoneyTransaction` là cha của `Payment` (tiền vào) và `Refund` (tiền ra), giữ các trường chung; mỗi lớp con tự cài `direction()`, `isSettled()`, `settledAt()`, `handledBy()`. `Booking.moneyHistory()` gộp hai loại thành sổ tiền nhờ đa hình. |
| **Value object** | `AgeRange`, `GeoPoint`, `TimeWindow`, `PenaltyTier` (record bất biến, tự kiểm tra dữ liệu). *Đã đổi:* không làm kiểu `Money`; tiền là số nguyên `long` tính bằng đồng (tiền Việt không có phần lẻ nên không cần `BigDecimal`). |

Bảng đầy đủ "nguyên lý → lớp nào, file nào" theo code thật nằm ở [`OOP.md`](OOP.md); sơ đồ lớp, ERD, use case, tuần tự ở `docs/diagrams/`.

### 3.4. Thiết kế API mới

- Tiền tố `/api/v1`, danh từ số nhiều, kebab-case: `/api/v1/tours`, `/api/v1/tours/{id}/departures`, `/api/v1/bookings/{code}`, `/api/v1/admin/bookings/{id}/refunds`.
- Một kiểu lỗi chung: `{ "code": "SEAT_NOT_ENOUGH", "message": "...", "fields": {...} }`.
- Phân trang chung: `?page=0&size=20` → `{ items, page, size, total }`.
- Giai đoạn cuối có **`docs/API_CHANGES.md`**: bảng đối chiếu ~65 API cũ frontend đang gọi → API mới.

## 4. Mô hình dữ liệu và thuật ngữ Việt–Anh

| Tiếng Việt (bản cũ) | Tên mới | Ghi chú |
|---|---|---|
| TaiKhoan, VaiTro | `Account`, `Role` | `Role` là enum (ADMIN, CUSTOMER) lưu trong cột `accounts.role`, không cần bảng riêng vì chỉ có 2 vai trò cố định |
| KhachHang | *(bỏ)* | Không cần bảng riêng: đơn lưu thẳng người liên hệ (tên, SĐT, email) và tài khoản nếu khách đã đăng nhập; khách vãng lai tra cứu bằng mã đơn + SĐT |
| KhuVuc, LoaiHinh, DichVu | `Region`, `TourCategory`, `Amenity` | Cùng kế thừa `NamedItem`; dùng chung `NamedItemService<T>` và một controller `/api/v1/{regions,tour-categories,amenities}` |
| DiemDen | `Destination` | **Thêm**: tọa độ, thời gian tham quan, giờ mở/đóng cửa, chi phí ước tính/người, độ nổi bật, thẻ sở thích |
| DiemDon | `PickupPoint` | |
| LoaiKhach | `PassengerType` | Khoảng tuổi, hệ số chiếm chỗ |
| PhuThu | `Surcharge` | |
| ChinhSachHuy, MocPhatHuy | `CancellationPolicy`, `PenaltyTier` | Có phiên bản, chốt vào đơn lúc đặt |
| Tour, HinhAnhTour | `Tour`, `TourImage` | Luôn đúng 1 ảnh đại diện |
| NgayLichTrinh, HoatDongLichTrinh | `Itinerary`, `ItineraryDay`, `ItineraryActivity` | `Itinerary` là bảng riêng: tour giữ lịch trình mẫu, đợt giữ bản sao tạo bằng `Itinerary.copy()` (mẫu Prototype) |
| DotKhoiHanh, BangGiaDot | `Departure`, `DeparturePrice` | `Departure` có cột `version` |
| DonDatTour, HanhKhach | `Booking`, `Passenger` | Giá chốt trên từng hành khách lúc đặt; `BookingStatus` là enum tự khai báo bước chuyển hợp lệ |
| ChiTietPhuThuDon, LichSuTrangThaiDon | `BookingSurcharge`, `BookingStatusHistory` | |
| ThanhToan, HoanTien | `Payment`, `Refund` | Mã tham chiếu thanh toán là duy nhất |
| YeuCauHuy, ChiTietHuyHanhKhach | `CancellationRequest`, `CancelledPassenger` | |
| LichSuChuyenDot | `DepartureTransfer` | |
| TinTuc, CauHinhTrang, LienHe | `Article`, `PageSetting`, `ContactMessage` | |
| *(mới)* | `SavedItinerary` | Lịch trình gợi ý khách đã lưu |

## 5. Gợi ý lịch trình — đã tìm hiểu

### 5.1. Bài toán này tên là gì

Trong nghiên cứu, việc "chọn điểm tham quan hợp sở thích và xếp thành lịch trình nhiều ngày, tôn trọng giờ mở cửa và quỹ thời gian mỗi ngày" gọi là **Tourist Trip Design Problem (TTDP)**. Mô hình toán phổ biến nhất cho nó là **Team Orienteering Problem with Time Windows (TOPTW)**:

- Mỗi điểm đến `i` có **điểm hấp dẫn** `S_i`, **thời gian tham quan** `T_i`, **khung giờ mở cửa** `[O_i, C_i]`.
- Có **thời gian di chuyển** `c_ij` giữa mọi cặp điểm.
- Mỗi ngày là 1 tuyến, xuất phát và kết thúc ở nơi lưu trú, không vượt quỹ giờ của ngày.
- Mục tiêu: **tổng điểm hấp dẫn của các điểm được chọn là lớn nhất**. Không bắt buộc đi hết mọi điểm (khác bài toán người giao hàng).

Bài toán này thuộc loại NP-khó (không có cách giải tối ưu nhanh khi dữ liệu lớn), nên dùng **heuristic** — cách giải gần đúng, nhanh, chất lượng tốt.

### 5.2. Thuật toán chọn: Iterated Local Search (ILS) của Vansteenwegen và cộng sự (2009)

Lý do chọn: được thiết kế đúng cho "hướng dẫn viên du lịch điện tử", chạy trong vài giây, chỉ gồm 2 bước dễ hiểu, dễ giải thích khi bảo vệ đồ án. Bọn mình có ~50–100 điểm đến nên sẽ chạy trong mili-giây.

**Bước chèn (Insertion)** — lặp tới khi không chèn thêm được:
- Với mỗi điểm chưa chọn, thử mọi vị trí trong mọi ngày, tính thời gian tăng thêm:
  `TimeInsertion_j = c_ij + Wait_j + T_j + c_jk − c_ik` (chèn `j` giữa `i` và `k`)
- Chèn được khi `TimeInsertion_j ≤ Wait_k + MaxDelay_k` và giờ bắt đầu tham quan nằm trong `[O_j, C_j]`.
  - `Wait_i = max(0, O_i − Arrival_i)`: thời gian phải chờ nếu tới sớm hơn giờ mở cửa.
  - `MaxDelay_i = min(C_i − Start_i, Wait_{i+1} + MaxDelay_{i+1})`: điểm `i` còn lùi giờ được bao lâu mà không làm hỏng các điểm sau.
- Chọn điểm có `Ratio = S_j² / TimeInsertion_j` lớn nhất (bình phương để ưu tiên điểm hấp dẫn hơn điểm chỉ "gần").

**Bước lắc (Shake)** — để thoát khỏi lời giải "kẹt":
- Bỏ `R` điểm liên tiếp trong mỗi ngày, bắt đầu từ vị trí `S`, dồn các điểm sau lên sớm hơn.
- Sau mỗi lần lắc: `S = S + R`, `R = R + 1`; quay vòng `S` khi vượt độ dài ngày ngắn nhất; reset `R = 1` khi `R = n / (3·m)` (n: số điểm, m: số ngày) hoặc khi tìm được lời giải tốt hơn.

**Dừng** khi 150 lần liên tiếp không cải thiện. Thuật toán không dùng số ngẫu nhiên → **cùng đầu vào cho cùng kết quả** (dễ test, dễ demo).

### 5.3. Áp dụng vào Vĩnh Long

| Thành phần TOPTW | Lấy từ đâu |
|---|---|
| Điểm hấp dẫn `S_i` | Tổng có trọng số của các `ScoringCriterion`: khớp sở thích khách chọn (thẻ của điểm đến), độ nổi bật (admin chấm 1–5), thuộc khu vực ưu tiên. Quy về số nguyên 1–100 |
| Thời gian tham quan `T_i` | Cột `visitMinutes` của điểm đến |
| Khung giờ `[O_i, C_i]` | Giờ mở/đóng cửa của điểm đến. *Đã đổi khi làm giai đoạn 7:* phải **tham quan xong trước giờ đóng cửa** (`C_i` = giờ đóng − thời gian tham quan), thực tế hơn "vào trước giờ đóng" |
| Thời gian di chuyển `c_ij` | Khoảng cách đường chim bay (Haversine) × 1,3 (hệ số đường vòng) ÷ 30 km/h. Đây là ước tính; có ghi chú `shortcut:` trong code, sau này có thể thay bằng bảng thời gian do admin nhập (ví dụ chỗ phải qua phà) |
| Quỹ giờ mỗi ngày | Nhịp đi: thong thả 8:00–16:00, vừa 7:30–17:30, dày 7:00–19:00 |
| Nơi xuất phát mỗi ngày | **Khách chọn nơi lưu trú**: chọn nhanh từ danh sách (trung tâm Vĩnh Long cũ, Bến Tre cũ, Trà Vinh cũ, cù lao An Bình) hoặc gửi tọa độ tự chọn trên bản đồ. Mỗi ngày đi và về nơi này |
| Bữa trưa | Thêm 1 "điểm ăn trưa" ảo mỗi ngày: 60 phút, bắt đầu trong 11:00–13:30, ăn tại chỗ vừa tham quan, buộc phải có. Tiền ăn ước tính 80.000 đ/người (chưa có danh mục quán ăn) |
| Ngân sách | Thêm điều kiện khi chèn: tổng chi phí ước tính (vé + ăn) × số người không vượt ngân sách |

**Kết quả trả về**: từng ngày → danh sách điểm với giờ đến, giờ bắt đầu, giờ rời, thời gian di chuyển; tổng chi phí ước tính; và **3 tour đang bán giống nhất** (so trùng điểm đến bằng hệ số Jaccard = số điểm chung ÷ số điểm hợp lại), kèm đợt khởi hành còn chỗ để khách bấm đặt.

**API**: `GET /api/v1/itineraries/lodging-presets`, `POST /api/v1/itineraries/suggest` (không cần đăng nhập), `POST /api/v1/itineraries` (lưu; máy chủ tự gợi ý lại từ tham số, không nhận lịch do client dựng), `GET /api/v1/me/itineraries`, `GET /api/v1/me/itineraries/{id}`, `DELETE /api/v1/me/itineraries/{id}`.

**Cài đặt** (gói `planner`): `IteratedLocalSearch` kiểm tra chèn được hay không bằng cách tính lại lịch cả ngày thay vì cập nhật dần Wait/MaxDelay như bài báo — vì bữa trưa ăn "tại chỗ", chèn một điểm ngay trước bữa trưa làm đổi luôn chỗ ăn, công thức cập nhật dần không còn đúng. Với 38 điểm vẫn chỉ mất 11–128 ms.
**Frontend**: cần thêm trang "Gợi ý lịch trình" (dev frontend làm theo Swagger).

### 5.4. Cách kiểm chứng

1. **Test bằng ví dụ có sẵn trong tài liệu**: ví dụ 4 địa điểm (Buijs, mục 4.1) có kết quả từng bước đã tính tay — sau bước chèn phải ra tuyến `0 → 3 → 1 → 0` với giờ đến 0, 5, 15, 25; sau bước lắc phải ra `0 → 3 → 0`. Test này đảm bảo cài đặt đúng công thức.
2. Test luật: không ngày nào vượt quỹ giờ, không vào điểm ngoài giờ mở cửa, không vượt ngân sách, luôn có bữa trưa.
3. So sánh với cách "tham lam" đơn giản (luôn chọn điểm điểm cao nhất kế tiếp): ILS phải cho tổng điểm ≥ tham lam trên dữ liệu mẫu. Số liệu này đưa vào báo cáo.

**Kết quả** (test `IlsVersusGreedyTests`, 38 điểm đến thật, 2 người lớn, không chọn sở thích nên điểm hấp dẫn chỉ theo độ nổi bật, không giới hạn ngân sách). Ví dụ tính tay của Buijs ra đúng từng số (`IteratedLocalSearchTest`); riêng bảng 3 của luận văn in nhầm (giờ về 22, MaxDelay 8) trong khi phép tính ngay sau dùng 20 và 10.

| Nơi lưu trú | Số ngày | Nhịp đi | Tham lam | Chỉ bước chèn | ILS | ILS hơn tham lam | Thời gian ILS (ms) |
|---|---|---|---|---|---|---|---|
| Trung tâm Vĩnh Long (cũ) | 1 | Thong thả | 100 | 300 | 300 | +200,0% | 30 |
| Trung tâm Vĩnh Long (cũ) | 1 | Đi dày | 275 | 400 | 400 | +45,5% | 27 |
| Trung tâm Vĩnh Long (cũ) | 2 | Thong thả | 325 | 525 | 525 | +61,5% | 35 |
| Trung tâm Vĩnh Long (cũ) | 2 | Đi dày | 650 | 725 | 825 | +26,9% | 56 |
| Trung tâm Vĩnh Long (cũ) | 3 | Thong thả | 650 | 700 | 700 | +7,7% | 62 |
| Trung tâm Vĩnh Long (cũ) | 3 | Đi dày | 875 | 1000 | 1100 | +25,7% | 128 |
| Trung tâm Bến Tre (cũ) | 1 | Thong thả | 200 | 200 | 200 | +0,0% | 11 |
| Trung tâm Bến Tre (cũ) | 1 | Đi dày | 325 | 400 | 450 | +38,5% | 25 |
| Trung tâm Bến Tre (cũ) | 2 | Thong thả | 450 | 500 | 500 | +11,1% | 52 |
| Trung tâm Bến Tre (cũ) | 2 | Đi dày | 700 | 675 | 900 | +28,6% | 76 |
| Trung tâm Bến Tre (cũ) | 3 | Thong thả | 550 | 675 | 675 | +22,7% | 49 |
| Trung tâm Bến Tre (cũ) | 3 | Đi dày | 925 | 1025 | 1125 | +21,6% | 75 |
| Trung tâm Trà Vinh (cũ) | 1 | Thong thả | 100 | 350 | 350 | +250,0% | 17 |
| Trung tâm Trà Vinh (cũ) | 1 | Đi dày | 325 | 450 | 475 | +46,2% | 26 |
| Trung tâm Trà Vinh (cũ) | 2 | Thong thả | 425 | 500 | 500 | +17,6% | 30 |
| Trung tâm Trà Vinh (cũ) | 2 | Đi dày | 575 | 725 | 875 | +52,2% | 47 |
| Trung tâm Trà Vinh (cũ) | 3 | Thong thả | 575 | 675 | 675 | +17,4% | 53 |
| Trung tâm Trà Vinh (cũ) | 3 | Đi dày | 875 | 950 | 1150 | +31,4% | 60 |
| Cù lao An Bình | 1 | Thong thả | 100 | 300 | 300 | +200,0% | 11 |
| Cù lao An Bình | 1 | Đi dày | 275 | 400 | 400 | +45,5% | 18 |
| Cù lao An Bình | 2 | Thong thả | 275 | 525 | 525 | +90,9% | 47 |
| Cù lao An Bình | 2 | Đi dày | 650 | 725 | 825 | +26,9% | 47 |
| Cù lao An Bình | 3 | Thong thả | 600 | 700 | 700 | +16,7% | 51 |
| Cù lao An Bình | 3 | Đi dày | 875 | 1000 | 1125 | +28,6% | 71 |

Đọc bảng: tham lam hay chọn ngay điểm nổi bật nhất dù ở xa và tham quan lâu, nên tốn cả buổi cho một điểm. Bước chèn đã khắc phục phần lớn nhờ tỉ số điểm²/thời gian chèn; bước lắc thêm được 5–25% ở các chuyến dài, nhiều điểm (cột "Chỉ bước chèn" so với "ILS"). Thời gian chạy thay đổi chút ít giữa các lần đo.

### 5.5. Dữ liệu điểm đến (đã thu thập)

`docs/data/diem-den.csv` có **38 điểm đến thật** (12 Vĩnh Long cũ, 13 Bến Tre cũ, 13 Trà Vinh cũ), tìm từ 8 nguồn trên mạng. Mỗi dòng ghi rõ nguồn và độ tin cậy của tọa độ, giờ mở cửa, giá vé. Chi tiết và danh sách 16 điểm còn thiếu tọa độ ở `docs/data/README.md`.

- 26/38 điểm có tọa độ đúng vị trí (OpenStreetMap, Wikipedia); 12 điểm là tọa độ gần đúng, cần nhóm chỉnh.
- Giờ mở cửa: chỉ 9 điểm có giờ cụ thể trong nguồn; 16 điểm nguồn ghi "mở cả ngày"; còn lại là ước tính.
- `visit_minutes` và `popularity` là mình ước tính.
- Giai đoạn 2 thêm cột **ngày đóng cửa trong tuần** (Văn Thánh Miếu đóng Chủ nhật).

### 5.6. Tài liệu tham khảo

- Vansteenwegen, Souffriau, Vanden Berghe, Van Oudheusden (2009), *Iterated local search for the team orienteering problem with time windows*, Computers & Operations Research.
- Buijs (2015), luận văn cử nhân Erasmus — cài đặt lại ILS trên, có ví dụ tính tay: https://thesis.eur.nl/pub/30280/Buijs.pdf
- Gunawan, Lau, Vansteenwegen (2016), *Orienteering Problem: A survey of recent variants, solution approaches and applications*, European Journal of Operational Research.
- Ruiz-Meza, Montoya-Torres (2022), *A systematic literature review for the tourist trip design problem*, Operations Research Perspectives, doi:10.1016/j.orp.2022.100228.
- Lim và cộng sự (2018), *Personalized trip recommendation for tourists based on user interests, points of interest visit durations and visit recency* (PersTour), Knowledge and Information Systems — cách cá nhân hóa điểm hấp dẫn theo sở thích.

## 6. Các lỗi của bản cũ sẽ chặn ngay từ thiết kế

| Lỗi bản cũ | Cách chặn |
|---|---|
| Duyệt hủy đơn đã hủy/hết hạn → trả chỗ 2 lần | Chuyển trạng thái qua `BookingStatus`; hủy đơn trực tiếp tự đóng yêu cầu hủy đang chờ |
| Trả tiền 2 lần không ai biết; cập nhật tiền đồng thời bị mất | Khóa dòng đơn khi ghi nhận tiền; tiền vượt tổng → cờ "CẦN HOÀN TIỀN" hiện trên dashboard; mỗi đơn chỉ 1 link VNPay đang chờ |
| Job hết hạn đọc dữ liệu cũ | Khóa đơn trước khi xét hết hạn; cùng thứ tự khóa (đơn → đợt) ở mọi nơi |
| Khóa JWT mặc định công khai | Không có giá trị mặc định; thiếu `JWT_SECRET` thì tự sinh ngẫu nhiên lúc chạy và cảnh báo |
| Khóa tài khoản chậm 24 giờ | Mỗi request kiểm tra trạng thái tài khoản; token sống 2 giờ |
| Không có test | Mỗi giai đoạn có test riêng (mục 7) |

## 7. Các giai đoạn

**Cách làm việc**: làm xong 1 giai đoạn → commit trên nhánh `hoanglong` → **dừng lại, báo cáo** (đã làm gì, test nào chạy, cách tự chạy thử) → bạn duyệt → mới sang giai đoạn sau.

| # | Giai đoạn | Nội dung | Xong khi |
|---|---|---|---|
| 0 | Khởi tạo | Maven + `mvnw`, Java 21, `docker-compose.yml` (lúc đầu PostgreSQL, nay là MySQL), Flyway, Swagger, xử lý lỗi chung, `/actuator/health`, README mới (bỏ XAMPP) | `docker compose up -d && ./mvnw verify` chạy xanh; Swagger mở được |
| 1 | Tài khoản | Đăng ký, đăng nhập, JWT, vai trò ADMIN/CUSTOMER, tạo admin lần đầu (mật khẩu từ biến môi trường), khóa/mở tài khoản, hồ sơ, đổi mật khẩu, đăng nhập Google | Test: sai mật khẩu, token hết hạn, tài khoản bị khóa bị chặn ngay |
| 2 | Danh mục | CRUD 8 loại danh mục; điểm đến có tọa độ, giờ mở cửa, ngày đóng cửa, thời gian tham quan, thẻ sở thích, cờ độ tin cậy dữ liệu; chính sách hủy có phiên bản; nạp 38 điểm đến từ `docs/data/diem-den.csv` | Test CRUD + validate (khoảng tuổi chồng nhau, giờ mở sau giờ đóng bị từ chối) |
| 3 | Tour & đợt | CRUD tour, ảnh, lịch trình mẫu, đợt khởi hành, bảng giá, điểm đón; API công khai tìm/lọc tour, chi tiết, lịch trình | Test lọc tour; tạo đợt tự sao chép lịch trình mẫu |
| 4 | Đặt tour | Báo giá, tạo đơn có khóa chỗ, job hết hạn giữ chỗ, tra cứu đơn (mã + SĐT), đơn của tôi, admin xem/lọc đơn | Test: tuổi tính theo ngày đi, đơn không có người lớn bị từ chối, 2 người tranh chỗ cuối chỉ 1 người được |
| 5 | Thanh toán | VNPay (tạo link, return, IPN — chống ghi trùng, kiểm chữ ký, số tiền), xác nhận thủ công kèm ảnh | Test chữ ký VNPay; IPN gửi 2 lần chỉ ghi 1 lần; trả thừa → cờ hoàn tiền |
| 6 | Hủy / hoàn / chuyển đợt | Hủy toàn bộ/từng người, phí phạt theo chính sách đã chốt, admin duyệt, hoàn tiền nhiều lần, chuyển đợt tính lại giá | Test: phí phạt theo mốc ngày; duyệt hủy đơn đã hủy bị từ chối; chuyển đợt trả chỗ cũ, giữ chỗ mới. *Ghi chú khi làm:* đơn chưa thu đồng nào thì hủy ngay không cần duyệt; mỗi đơn chỉ một yêu cầu hủy đang chờ; hủy một phần phải còn người lớn; chuyển sang đợt đắt hơn thì đơn đã trả đủ quay về "đã cọc"; khóa hai đợt theo thứ tự mã tăng dần để hai admin chuyển ngược chiều không chờ nhau mãi |
| 7 | Gợi ý lịch trình | ILS mục 5, bữa trưa, ngân sách, lưu lịch trình, gợi ý tour giống nhất | Test ví dụ tính tay (5.4) ra đúng từng số; các test luật; bảng so sánh ILS với tham lam |
| 8 | Nội dung & quản trị | Tin tức, cấu hình trang, liên hệ, upload ảnh, dashboard (doanh thu, đơn theo trạng thái, đơn cần hoàn tiền), quản lý người dùng | Upload chỉ nhận ảnh; dashboard đếm đúng. *Ghi chú khi làm:* quản lý người dùng (tìm, khóa/mở) đã có từ giai đoạn 1 nên không làm thêm; doanh thu = tiền thanh toán thành công − tiền đã hoàn, tính theo ngày tiền vào/ra; nhận diện ảnh bằng byte đầu file, không tin đuôi file |
| 9 | Bàn giao | Dữ liệu demo (chỉ bật ở profile `dev`), `docs/API_CHANGES.md`, sơ đồ lớp + ERD (draw.io), README hoàn chỉnh | Máy mới clone về chạy được bằng 3 lệnh trong README. *Ghi chú khi làm:* dữ liệu demo nạp qua các service (được kiểm tra như admin nhập tay) và chỉ khi CSDL chưa có tour; ảnh tour/bài viết nhận thêm đường dẫn `/images/...` (ảnh có sẵn trong frontend); ERD sinh tự động từ các file migration, sơ đồ lớp chỉ vẽ các lớp chính |

## 8. Không làm (trừ khi bạn yêu cầu)

- Gửi email/SMS thông báo.
- Hoàn tiền tự động qua API VNPay (admin ghi nhận hoàn tiền thủ công).
- Gợi ý lịch trình bằng AI/LLM, hoặc học sở thích từ lịch sử người dùng.
- Thời gian di chuyển thật từ Google Maps (cần API key trả phí).
- Sửa frontend — dev frontend làm theo `docs/API_CHANGES.md` và Swagger.
- Triển khai lên server thật.

## 9. Đã quyết định

| Câu hỏi | Quyết định |
|---|---|
| Tên trong code | Tiếng Anh, Javadoc tiếng Việt ở đầu mỗi class |
| Phiên bản Java | Java 21, cả nhóm cập nhật |
| Gợi ý lịch trình | TOPTW + Iterated Local Search (mục 5) |
| Cách làm việc | Dừng sau mỗi giai đoạn để duyệt |
| Dữ liệu điểm đến | Dữ liệu thật tìm trên mạng: `docs/data/diem-den.csv` |
| Nơi xuất phát mỗi ngày | Khách chọn nơi lưu trú |
| CSDL | MySQL 8.4 qua Docker (đổi từ PostgreSQL). Bảng mã `utf8mb4`, so sánh `utf8mb4_0900_as_ci`: không phân biệt hoa/thường nhưng phân biệt dấu tiếng Việt. Khác biệt đã xử lý: khóa ngoại viết thành dòng riêng (MySQL bỏ qua khóa ngoại viết trên dòng cột); "mỗi đơn một yêu cầu hủy đang chờ" dùng cột sinh tự động + unique thay cho index có điều kiện; thay lịch trình sửa ngày cũ tại chỗ vì MySQL không có ràng buộc kiểm tra lúc commit; nội dung dài dùng `mediumtext` |

Đã làm xong cả 10 giai đoạn (giai đoạn 0–9) trên nhánh `hoanglong`.
