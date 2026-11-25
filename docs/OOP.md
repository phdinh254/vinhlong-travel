# Áp dụng lập trình hướng đối tượng trong VinhLong Tour

Tài liệu cho báo cáo môn **Lập trình ứng dụng hướng đối tượng**: mỗi nguyên lý được dùng ở lớp nào, file nào, để giải quyết việc gì. Mọi tên lớp và phương thức dưới đây đều có thật trong code. Đường dẫn tính từ `backend/src/main/java/vn/edu/tvu/vinhlongtour/`.

Nguyên tắc chung của dự án: chỉ dùng kế thừa, interface hay mẫu thiết kế ở chỗ nghiệp vụ thật sự cần, không thêm lớp chỉ để "trông OOP".

Sơ đồ đi kèm (trong `docs/diagrams/`, mở bằng draw.io):

| Sơ đồ | File |
|---|---|
| Sơ đồ lớp (các lớp chính) | `class-diagram.drawio.png` |
| ERD (37 bảng) | `erd.drawio.png` |
| Use case | `use-case.drawio.png` |
| Tuần tự: đặt tour | `sequence-booking.drawio.png` |
| Tuần tự: thanh toán payOS (tạo link, webhook, trang kết quả) | `sequence-payment.drawio.png` |
| Tuần tự: gợi ý lịch trình | `sequence-planner.drawio.png` |
| Trạng thái của đơn (`BookingStatus`) | `state-booking.drawio.png` |
| Hoạt động: hủy tour và hoàn tiền | `activity-cancellation.drawio.png` |
| Triển khai bằng Docker | `deployment.drawio.png` |

## 1. Đóng gói (encapsulation)

Dữ liệu của entity để `private`; muốn thay đổi phải gọi phương thức nghiệp vụ, phương thức đó tự kiểm tra quy tắc. Service không gán thẳng số chỗ hay số tiền, nên không có đường nào làm sai lệch dữ liệu.

| Lớp | Phương thức | Quy tắc được giữ bên trong |
|---|---|---|
| `tour/domain/Departure` | `reserveSeats(n)`, `releaseSeats(n)`, `changeCapacity(n)` | Không giữ quá số chỗ còn (`SEAT_NOT_ENOUGH`); không giảm sức chứa dưới số chỗ đã đặt |
| `booking/domain/Booking` | `recordManualPayment(...)`, `refund(...)`, `requestCancellation(...)`, `approveCancellation(...)`, `transferTo(...)` | Tự cộng/trừ tiền, tự đổi trạng thái; không hoàn quá số tiền cần hoàn; mỗi đơn chỉ một yêu cầu hủy đang chờ; hủy một phần phải còn người lớn |
| `auth/domain/Account` | `lock()`, `unlock()`, `changePasswordHash(...)` | Không có setter cho vai trò hay trạng thái |
| `catalog/domain/CancellationPolicy` | `penaltyPercentFor(days)`, `update(...)` | Chính sách đã có hiệu lực thì không sửa được (`POLICY_ALREADY_EFFECTIVE`); các mốc phạt không được chồng nhau |
| `tour/domain/Tour` | `changeStatus(...)`, `ensureSellable()` | Chỉ mở bán khi có ảnh và lịch trình đủ số ngày |

Các lớp con của entity (`Passenger`, `Payment`, `CancellationRequest`...) có constructor hoặc phương thức tạo ở phạm vi gói (package-private), nên chỉ `Booking` tạo được chúng.

## 2. Kế thừa (inheritance)

| Lớp cha | Lớp con | Dùng chung được gì |
|---|---|---|
| `catalog/domain/NamedItem` (trừu tượng) | `Region`, `TourCategory`, `Amenity` | Trường `id`, `name`, phương thức `rename()`. Cả ba dùng chung **một** service generic `NamedItemService<T extends NamedItem>` và **một** controller `NamedItemController` cho đường dẫn `/api/v1/{regions,tour-categories,amenities}` |
| `booking/domain/MoneyTransaction` (trừu tượng) | `Payment`, `Refund` | Trường chung của mọi khoản tiền: đơn, số tiền, phương thức, mã giao dịch, ảnh chứng từ, ghi chú; phương thức `signedAmount()` |
| `RuntimeException` | `common/ApiException` | Mọi lỗi nghiệp vụ mang theo mã lỗi và mã HTTP |
| `ResponseEntityExceptionHandler` (Spring) | `common/GlobalExceptionHandler` | Ghi đè cách trả lỗi chuẩn của Spring để mọi lỗi có cùng dạng `{ code, message, fields }` |

## 3. Đa hình (polymorphism)

| Ở đâu | Cùng một lời gọi | Mỗi kiểu xử lý khác nhau |
|---|---|---|
| `planner/scoring/ScoringCriterion` | `criterion.value(destination)` | `InterestMatch` tính theo sở thích khách chọn, `Popularity` theo độ nổi bật, `PreferredRegion` theo khu vực ưu tiên. `ScoringCriterion.score(...)` cộng có trọng số mà không cần biết đang là tiêu chí nào |
| `booking/domain/MoneyTransaction` | `direction()`, `isSettled()`, `settledAt()`, `handledBy()` | `Payment`: tiền vào, chỉ tính khi thanh toán thành công. `Refund`: tiền ra, tính ngay. `Booking.moneyHistory()` gộp hai loại thành **sổ tiền** của đơn (trả về trong API chi tiết đơn, trường `moneyHistory`) |
| `catalog/domain/SurchargeMethod` (enum) | `method.quantity(passengers, units)` | Mỗi giá trị tự cài phương thức trừu tượng: `PER_PERSON` tính theo số khách, `PER_BOOKING` tính 1 lần, `PER_UNIT` tính theo số lượng khách chọn |
| `booking/domain/BookingStatus` (enum) | `status.canMoveTo(target)`, `holdsSeats()` | Mỗi trạng thái tự biết được chuyển tới đâu; chuyển sai báo lỗi ngay |
| `content/domain/SettingKey` (enum) | `key.isLink()` | Mục cấu hình dạng link bị kiểm tra phải là `http(s)://` |

**Ghi đè** (`@Override`): các lớp con của `MoneyTransaction`, các tiêu chí chấm điểm, các giá trị của `SurchargeMethod`, `GlobalExceptionHandler`.
**Nạp chồng**: `ApiError` có 2 constructor (có hoặc không có danh sách lỗi từng ô).

## 4. Trừu tượng (abstraction)

- **Lớp trừu tượng:** `NamedItem`, `MoneyTransaction`; phương thức trừu tượng `SurchargeMethod.quantity(...)`.
- **Interface:** `ScoringCriterion`; các repository (`BookingRepository`, `TourRepository`...) là interface, Spring Data tự tạo phần cài đặt.
- **Ẩn chi tiết thuật toán:** `planner/solver/IteratedLocalSearch` chỉ lộ ra `solve()`. Bên trong là bước chèn và bước lắc; nơi gọi (`TripPlannerService`) không cần biết.

## 5. Value object (đối tượng giá trị bất biến)

`AgeRange`, `GeoPoint`, `TimeWindow`, `PenaltyTier`, `TourImage` là `record` (bất biến). Constructor tự kiểm tra dữ liệu, ví dụ tọa độ ngoài phạm vi báo `INVALID_COORDINATES`, giờ mở sau giờ đóng báo `INVALID_OPENING_HOURS`. Các record này có thêm hành vi: `AgeRange.contains(age)`, `AgeRange.overlaps(other)`. Kết quả trả về từ payOS cũng là record: `PayOsGateway.CreatedLink`, `PayOsGateway.PaymentInfo` (có `isPaid()`, `isClosedWithoutPayment()`).

## 6. Mẫu thiết kế

| Mẫu | Ở đâu | Giải quyết việc gì |
|---|---|---|
| **Strategy** | `ScoringCriterion` và 3 cài đặt | Thêm tiêu chí chấm điểm mới chỉ cần thêm lớp, không sửa thuật toán |
| **Prototype** | `Itinerary.copy()`, gọi trong constructor của `Departure` | Mỗi đợt khởi hành giữ bản sao riêng của lịch trình mẫu; sửa tour không làm đổi các đợt đã tạo |
| **State** (bằng enum) | `BookingStatus` | Quy định các bước chuyển trạng thái hợp lệ của đơn ở một chỗ (vẽ ở `state-booking.drawio.png`) |
| **Template / hook** | `MoneyTransaction.signedAmount()` dùng `direction()` do lớp con cài | Phần chung viết một lần ở lớp cha, phần khác nhau để lớp con quyết định |
| **Static factory** | `ApiException.badRequest(...)`, `notFound(...)`, `conflict(...)`; `Payment.online(...)`, `Payment.manual(...)` | Tạo đối tượng đúng trạng thái ban đầu, tên hàm nói rõ ý nghĩa |
| **Aggregate** | `Booking` sở hữu `Passenger`, `Payment`, `Refund`, `CancellationRequest`, `DepartureTransfer` | Mọi thay đổi đi qua `Booking`, khóa dòng đơn một lần là an toàn cho cả nhóm dữ liệu |
| **Repository** | các interface `*Repository` | Tách truy vấn CSDL khỏi nghiệp vụ |
| **Dependency Injection** | constructor của mọi service/controller | Lớp nhận phụ thuộc từ ngoài vào, dễ thay bằng đối tượng giả khi test (`AccountServiceTest`) |
| **DTO** | các file `*Dtos.java` | Dữ liệu API tách khỏi entity; đổi CSDL không làm vỡ API |

## 7. Generic, collection, xử lý ngoại lệ

- **Generic:** `NamedItemService<T extends NamedItem>`, `PageResponse<T>`, `Lookups.all/one` (hàm generic dùng cho mọi repository).
- **Collection và Stream:** sổ tiền `Booking.moneyHistory()` gộp hai danh sách bằng `Stream.concat`, lọc, sắp xếp; thuật toán gợi ý dùng `List`, `Set`, mảng hai chiều thời gian đi đường.
- **Ngoại lệ:** lỗi nghiệp vụ ném `ApiException` có mã lỗi; `GlobalExceptionHandler` bắt mọi loại lỗi và trả về một dạng duy nhất; lỗi không mong đợi không lộ chi tiết bên trong.

## 8. Các nguyên tắc SOLID dễ thấy

- **S – mỗi lớp một việc:** `PriceCalculator` chỉ tính giá; `PayOsGateway` chỉ nói chuyện với payOS (ký yêu cầu, gọi API, kiểm chữ ký webhook), còn ghi nhận tiền là việc của `PaymentService` và `Booking`; `ImageStorage` chỉ lưu ảnh.
- **O – mở để mở rộng, đóng để sửa:** thêm tiêu chí chấm điểm, thêm cách tính phụ thu hay thêm loại khoản tiền đều là thêm lớp/giá trị mới, không sửa code đang chạy.
- **L – thay lớp con cho lớp cha:** ở đâu cần `MoneyTransaction` thì `Payment` hay `Refund` đều dùng được; ở đâu cần `NamedItem` thì `Region`, `TourCategory`, `Amenity` đều dùng được.
- **D – phụ thuộc vào trừu tượng:** service phụ thuộc interface repository, `TripPlannerService` làm việc với danh sách `ScoringCriterion`.

## 9. Bằng chứng chạy đúng

- 151 test tự động (`./mvnw verify`), gồm test riêng cho từng quy tắc ở trên, ví dụ: tổng sổ tiền luôn bằng số tiền đơn đang giữ (`BookingCancellationTest`), ví dụ tính tay của thuật toán gợi ý ra đúng từng số (`IteratedLocalSearchTest`), thanh toán payOS chạy với một payOS giả dựng ngay trong test (`PaymentIntegrationTests`).
- Bộ Postman 142 request (`backend/postman/VinhLong Tour API`).
