# Dữ liệu điểm đến — tỉnh Vĩnh Long (mới)

File `diem-den.csv` (UTF-8, phân cách bằng dấu phẩy) gồm **38 điểm đến**: 12 ở khu vực Vĩnh Long cũ, 13 ở Bến Tre cũ, 13 ở Trà Vinh cũ.
Dữ liệu được tìm trên mạng ngày 13/11/2026. Ở giai đoạn 2, file này đã được nạp vào CSDL qua `backend/src/main/resources/db/migration/V3__seed_destinations.sql`; từ đó trở đi admin sửa dữ liệu trực tiếp trên web, sửa file CSV không làm thay đổi CSDL.

> **Đừng mở file bằng Excel rồi bấm Save.** Excel ở máy cài tiếng Việt dùng dấu `;` làm dấu phân cách nên sẽ làm hỏng định dạng (đã xảy ra một lần). Muốn xem thì dùng Excel → Data → From Text/CSV, chọn dấu phẩy; muốn sửa thì sửa bằng VS Code hoặc Google Sheets.

> **Cần nhóm kiểm tra trước khi bảo vệ đồ án.** Các nguồn chủ yếu là blog du lịch, mỗi nguồn cập nhật một thời điểm khác nhau. Mọi dòng có cờ `GAN_DUNG`, `UOC_TINH`, `CHUA_XAC_MINH`, `CHUA_RO` đều cần người đi thực tế hoặc gọi điện xác nhận.

## Ý nghĩa các cột

| Cột | Ý nghĩa |
|---|---|
| `code` | Mã điểm: `VL..` Vĩnh Long cũ, `BT..` Bến Tre cũ, `TV..` Trà Vinh cũ |
| `old_area` | Khu vực tỉnh cũ: `VINH_LONG`, `BEN_TRE`, `TRA_VINH` |
| `address` | Địa chỉ theo tên xã/phường **sau sáp nhập** (khi nguồn có), kèm tên cũ trong ngoặc |
| `lat`, `lng` | Vĩ độ, kinh độ |
| `coord_quality` | `OSM`: đúng vị trí điểm trên OpenStreetMap · `WIKI`: tọa độ trong bài Wikipedia tiếng Việt · `GAN_DUNG`: lấy theo tâm xã/ấp hoặc điểm lân cận, **lệch có thể vài km** |
| `tags` | Thẻ sở thích, cách nhau bằng `;`: `sinh-thai`, `van-hoa`, `tam-linh`, `lich-su`, `lang-nghe`, `bien`, `cho-am-thuc`, `vui-choi`, `khac` (trong CSDL là bảng `tour_categories`) |
| `open_time`, `close_time` | Giờ mở/đóng cửa (dùng cho khung giờ trong gợi ý lịch trình) |
| `hours_quality` | `NGUON`: có trong nguồn · `CA_NGAY`: nguồn ghi "mở cả ngày", app quy về 06:00–18:00 · `UOC_TINH`: không có nguồn, mình ước tính · `CHUA_XAC_MINH`: chỉ thấy trong tóm tắt kết quả tìm kiếm, chưa mở được trang gốc |
| `ticket_adult`, `ticket_child` | Giá vé (đồng). `0` = miễn phí. Trống = không rõ. Khi nguồn ghi một khoảng giá thì lấy mức cao, để không vượt ngân sách của khách |
| `price_quality` | Như `hours_quality`, thêm `CHUA_RO` = không tìm được giá |
| `visit_minutes` | Thời gian tham quan đề xuất — **mình ước tính** theo loại điểm (chùa 30–45', bảo tàng 60', cù lao/khu sinh thái 120–180') |
| `popularity` | Độ nổi bật 1–5 — **mình ước tính** theo số nguồn nhắc tới điểm đó |
| `sources` | Mã nguồn, xem bảng dưới |

## Nguồn

| Mã | Nguồn |
|---|---|
| NCMK | [Nụ Cười Mekong — 33 địa điểm du lịch Vĩnh Long sau sáp nhập](https://nucuoimekong.com/dia-diem-du-lich-vinh-long) (địa chỉ theo tên xã/phường mới) |
| MYTOUR | [Mytour — 23 điểm đến du lịch hấp dẫn ở Vĩnh Long](https://mytour.vn/vi/blog/bai-viet/23-diem-du-lich-hap-dan-o-vinh-long.html) |
| MIA | [MIA.vn — Vĩnh Long có gì chơi](https://mia.vn/cam-nang-du-lich/vinh-long-co-gi-choi-15293) |
| BHX | [Bách Hóa Xanh — 22 địa điểm du lịch Bến Tre](https://www.bachhoaxanh.com/kinh-nghiem-hay/10-dia-diem-du-lich-ben-tre-thu-vi-ban-khong-the-bo-lo-1362101) |
| DLCP | [Du lịch Cồn Phụng](https://dulichconphung.com/) (giá vé tàu) |
| LHVN | [Lữ hành Việt Nam — điểm check-in Bến Tre](https://luhanhvietnam.com.vn/du-lich/7-diem-check-in-dep-o-ben-tre-hut-khach.html) |
| INTOUR | [Intour — bảng giá vé tham quan Trà Vinh](https://intour.vn/kinh-nghiem-du-lich/update-bang-gia-ve-cac-diem-tham-quan-tai-tra-vinh-moi-nhat-1686.html) (bài gốc đăng 2022) |
| SGSTAR | [Saigon Star Travel — tour Trà Vinh 1 ngày](https://saigonstartravel.com/tour-du-lich-tra-vinh-1-ngay) |
| TIMKIEM | Tóm tắt kết quả tìm kiếm (Traveloka, Mytour blog, Vua Nệm); trang gốc chưa mở được |
| OSM | OpenStreetMap, tra qua [Photon](https://photon.komoot.io) |
| WIKI | Wikipedia tiếng Việt |

## Chưa đưa vào (thiếu tọa độ đáng tin)

Các điểm dưới đây có trong nguồn nhưng mình không tìm được tọa độ. Nhóm bổ sung tọa độ (chuột phải trên Google Maps → bấm vào dòng số đầu tiên để chép) rồi thêm vào file:

- **Vĩnh Long cũ:** Chùa Ông – Thất Phủ Miếu (22 Nguyễn Chí Thanh), Chùa Phật Ngọc Xá Lợi (287A QL80, Tân Ngãi), Chùa Phước Hậu – vườn kinh đá (QL54, Ngãi Tứ), Đền thờ / Khu lưu niệm Phạm Hùng (QL53, Long Phước), Khu du lịch Trường Huy, Khu du lịch Hoàng Hảo, Khu sinh thái Nhà Xưa, Chùa Hạnh Phúc Tăng, Chùa cổ Long An (Trà Ôn).
- **Bến Tre cũ:** Khu du lịch Lan Vương, Du lịch sinh thái Phú An Khang, Chùa Bạch Vân, Chùa Vạn Phước (Bình Đại), Khu di tích Sài Gòn – Gia Định (Tân Phú Tây).
- **Trà Vinh cũ:** Đền thờ Chủ tịch Hồ Chí Minh (Long Đức), Khu tưởng niệm Anh hùng Nguyễn Thị Út (Tam Ngãi).

## Điểm đáng chú ý khi dùng cho gợi ý lịch trình

- **Chợ nổi Trà Ôn** đặt khung 05:00–09:00 vì chợ nổi họp buổi sáng, dù nguồn ghi "cả ngày".
- **Văn Thánh Miếu** đóng cửa Chủ nhật — đã ghi vào bảng `destination_closed_days` khi nạp (CSV không có cột này).
- **Cồn Phụng, cù lao An Bình, Cồn Chim** phải đi thuyền/phà; thời gian di chuyển ước tính theo đường chim bay sẽ thấp hơn thực tế.
