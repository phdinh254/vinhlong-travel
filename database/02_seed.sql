/* =====================================================================
   DỮ LIỆU KHỞI TẠO / DỮ LIỆU MẪU (MySQL / MariaDB)
   - Danh mục (vai trò, loại hình, khu vực, loại khách, dịch vụ...) dùng được ngay.
   - Tour, đợt khởi hành, giá: là DỮ LIỆU MẪU để chạy thử, cần thay bằng số liệu thật.
   - Tài khoản ADMIN được backend tự tạo khi khởi động lần đầu (xem application.yml).
   ===================================================================== */
USE vinhlongtour;
SET NAMES utf8mb4;

INSERT INTO VaiTro (MaVaiTro, TenVaiTro) VALUES
 ('ADMIN', 'Quản trị viên'),
 ('KHACH_HANG', 'Khách hàng');

/* Tỉnh Vĩnh Long (mới) hợp nhất từ Vĩnh Long, Bến Tre, Trà Vinh (hiệu lực 01/7/2025) */
INSERT INTO KhuVuc (KhuVucID, TenKhuVuc, MoTa) VALUES
 (1, 'Khu vực Vĩnh Long (cũ)', 'Cù lao An Bình, làng nghề gốm đỏ Mang Thít, Văn Thánh Miếu...'),
 (2, 'Khu vực Bến Tre (cũ)',   'Xứ dừa, cồn Phụng, cồn Quy, di tích Đồng Khởi...'),
 (3, 'Khu vực Trà Vinh (cũ)',  'Văn hóa Khmer, Ao Bà Om, chùa Âng, biển Ba Động...');

INSERT INTO LoaiHinh (LoaiHinhID, TenLoaiHinh, MoTa) VALUES
 (1, 'Sinh thái miệt vườn', 'Tham quan vườn trái cây, đi xuồng kênh rạch'),
 (2, 'Văn hóa - Lịch sử',   'Di tích, đình chùa, bảo tàng'),
 (3, 'Làng nghề truyền thống', 'Gốm, kẹo dừa, đan lát...'),
 (4, 'Ẩm thực',             'Trải nghiệm đặc sản địa phương'),
 (5, 'Biển - Nghỉ dưỡng',   'Biển, khu nghỉ dưỡng'),
 (6, 'Homestay - Trải nghiệm', 'Lưu trú nhà dân, làm nông cùng người dân');

/* Q18-Q19: quy tắc độ tuổi và hệ số chiếm chỗ */
INSERT INTO LoaiKhach (LoaiKhachID, TenLoaiKhach, TuoiTu, TuoiDen, HeSoChiemCho) VALUES
 (1, 'Người lớn', 12, 150, 1),
 (2, 'Trẻ em',     5,  11, 1),
 (3, 'Em bé',      0,   4, 0);

INSERT INTO DichVu (DichVuID, TenDichVu, BieuTuong) VALUES
 (1, 'Xe đưa đón', 'bus'),
 (2, 'Hướng dẫn viên', 'guide'),
 (3, 'Vé tham quan', 'ticket'),
 (4, 'Bữa ăn theo chương trình', 'meal'),
 (5, 'Khách sạn / Homestay', 'hotel'),
 (6, 'Bảo hiểm du lịch', 'shield'),
 (7, 'Nước uống, khăn lạnh', 'water'),
 (8, 'Thuế VAT', 'tax'),
 (9, 'Chi phí cá nhân', 'wallet'),
 (10, 'Tiền tip HDV và tài xế', 'tip');

INSERT INTO DiemDen (DiemDenID, TenDiemDen, KhuVucID, DiaChi, MoTa) VALUES
 (1, 'Cù lao An Bình', 1, 'Huyện Long Hồ (cũ), Vĩnh Long', 'Cù lao miệt vườn với vườn trái cây, nhà cổ.'),
 (2, 'Văn Thánh Miếu Vĩnh Long', 1, 'Phường 4 (cũ), TP. Vĩnh Long', 'Di tích kiến trúc nghệ thuật cấp quốc gia.'),
 (3, 'Làng nghề gạch gốm Mang Thít', 1, 'Huyện Mang Thít (cũ), Vĩnh Long', 'Quần thể lò gạch gốm đỏ ven sông Cổ Chiên.'),
 (4, 'Cồn Phụng', 2, 'Huyện Châu Thành (cũ), Bến Tre', 'Cồn nổi trên sông Tiền, gắn với Đạo Dừa.'),
 (5, 'Khu di tích Đồng Khởi', 2, 'Huyện Mỏ Cày Nam (cũ), Bến Tre', 'Di tích lịch sử phong trào Đồng Khởi.'),
 (6, 'Ao Bà Om', 3, 'Phường 8 (cũ), TP. Trà Vinh', 'Thắng cảnh gắn với truyền thuyết người Khmer.'),
 (7, 'Chùa Âng', 3, 'Phường 8 (cũ), TP. Trà Vinh', 'Ngôi chùa Khmer cổ, cạnh Ao Bà Om.'),
 (8, 'Biển Ba Động', 3, 'Thị xã Duyên Hải (cũ), Trà Vinh', 'Bãi biển hoang sơ.');

INSERT INTO DiemDon (DiemDonID, TenDiemDon, DiaChi, KhuVucID) VALUES
 (1, 'Bến xe Vĩnh Long', 'TP. Vĩnh Long (cũ)', 1),
 (2, 'Trung tâm TP. Bến Tre (cũ)', 'Bến Tre', 2),
 (3, 'Trung tâm TP. Trà Vinh (cũ)', 'Trà Vinh', 3),
 (4, 'TP. Hồ Chí Minh - Quận 1', 'TP. Hồ Chí Minh', NULL);

INSERT INTO CauHinhTrang (KhoaCauHinh, GiaTri) VALUES
 ('GIOI_THIEU', '<p>Chúng tôi chuyên tổ chức các tour khám phá tỉnh Vĩnh Long (mới) – vùng đất hội tụ miệt vườn sông nước, xứ dừa và văn hóa Khmer đặc sắc.</p>'),
 ('HOTLINE', '0000 000 000'),
 ('EMAIL', 'lienhe@example.com'),
 ('DIA_CHI', 'Vĩnh Long'),
 ('BAN_DO_NHUNG', '');

/* Chính sách hủy chung (Q37) - MẪU */
INSERT INTO ChinhSachHuy (ChinhSachHuyID, TourID, TenChinhSach, PhienBan, TuNgay, NoiDung)
VALUES (1, NULL, 'Chính sách hủy chung', 1, '2025-01-01', 'Áp dụng cho mọi tour không có chính sách riêng.');
INSERT INTO MocPhatHuy (ChinhSachHuyID, SoNgayTruocTu, SoNgayTruocDen, PhanTramPhat) VALUES
 (1, 15, 9999, 0),
 (1, 7, 14, 30),
 (1, 3, 6, 50),
 (1, 0, 2, 100);

INSERT INTO PhuThu (TourID, TenPhuThu, CachTinh, DonGia) VALUES
 (NULL, 'Phòng đơn (homestay/khách sạn)', 'THEO_SO_LUONG', 250000),
 (NULL, 'Xuất hóa đơn VAT', 'THEO_DON', 0),
 (NULL, 'Bảo hiểm nâng cao', 'THEO_NGUOI', 30000);

/* ---------------- TOUR MẪU ---------------- */
INSERT INTO Tour (TourID, MaTour, TenTour, MoTa, ThoiLuong, SoNgay, TrangThai, NoiBat) VALUES
 (1, 'VL-AB-1N', 'Cù lao An Bình – Miệt vườn Vĩnh Long 1 ngày', 'Đi xuồng kênh rạch, thăm vườn trái cây, làng gốm Mang Thít.', '1 ngày', 1, 'DANG_BAN', 1),
 (2, 'BT-DUA-2N', 'Xứ dừa Bến Tre – Cồn Phụng 2 ngày 1 đêm', 'Trải nghiệm làng nghề kẹo dừa, nghỉ homestay, thăm di tích Đồng Khởi.', '2 ngày 1 đêm', 2, 'DANG_BAN', 1),
 (3, 'TV-KHMER-2N', 'Văn hóa Khmer Trà Vinh – Biển Ba Động 2 ngày 1 đêm', 'Ao Bà Om, chùa Âng, biển Ba Động.', '2 ngày 1 đêm', 2, 'DANG_BAN', 0);

INSERT INTO Tour_LoaiHinh VALUES (1,1),(1,3),(2,1),(2,3),(2,6),(3,2),(3,5);
INSERT INTO Tour_KhuVuc VALUES (1,1),(2,2),(3,3);
INSERT INTO Tour_DichVu (TourID, DichVuID, LoaiApDung) VALUES
 (1,1,'BAO_GOM'),(1,2,'BAO_GOM'),(1,3,'BAO_GOM'),(1,4,'BAO_GOM'),(1,9,'KHONG_BAO_GOM'),(1,8,'KHONG_BAO_GOM'),
 (2,1,'BAO_GOM'),(2,2,'BAO_GOM'),(2,4,'BAO_GOM'),(2,5,'BAO_GOM'),(2,6,'BAO_GOM'),(2,9,'KHONG_BAO_GOM'),
 (3,1,'BAO_GOM'),(3,2,'BAO_GOM'),(3,5,'BAO_GOM'),(3,10,'KHONG_BAO_GOM');

/* Ảnh tour (Q06): tải ảnh thật trong trang Quản trị > Tour > Hình ảnh. */

/* Lịch trình mẫu (Q08-Q12) */
INSERT INTO NgayLichTrinh (NgayLichTrinhID, TourID, NgayThuMay, TieuDeNgay) VALUES
 (1, 1, 1, 'Khám phá cù lao An Bình'),
 (2, 2, 1, 'Cồn Phụng – Làng nghề dừa'),
 (3, 2, 2, 'Di tích Đồng Khởi'),
 (4, 3, 1, 'Ao Bà Om – Chùa Âng'),
 (5, 3, 2, 'Biển Ba Động');
INSERT INTO HoatDongLichTrinh (NgayLichTrinhID, LoaiHoatDong, GioBatDau, GioKetThuc, NoiDung, ThuTuHienThi, DiemDenID) VALUES
 (1,'DI_CHUYEN','07:00','08:00','Đón khách tại điểm hẹn, khởi hành.',1,NULL),
 (1,'THAM_QUAN','08:30','11:00','Đi xuồng qua kênh rạch, tham quan vườn trái cây.',2,1),
 (1,'AN_UONG','11:30','13:00','Ăn trưa đặc sản miệt vườn.',3,NULL),
 (1,'THAM_QUAN','14:00','16:00','Tham quan làng nghề gạch gốm Mang Thít.',4,3),
 (1,'DI_CHUYEN','16:00','17:30','Trả khách, kết thúc chương trình.',5,NULL),
 (2,'THAM_QUAN','08:30',NULL,'Tham quan Cồn Phụng, cơ sở kẹo dừa.',1,4),
 (3,'THAM_QUAN','08:00',NULL,'Tham quan Khu di tích Đồng Khởi.',1,5),
 (4,'THAM_QUAN','09:00',NULL,'Tham quan Ao Bà Om và chùa Âng.',1,6),
 (5,'THAM_QUAN','08:00',NULL,'Tắm biển Ba Động.',1,8);

/* Đợt khởi hành mẫu: mỗi tour 2 đợt, sau ngày chạy script 14 và 28 ngày, khởi hành 07:00 */
INSERT INTO DotKhoiHanh (DotKhoiHanhID, TourID, MaDot, NgayGioDi, NgayGioVe, SucChuaToiDa, HanNhanKhach, TrangThai)
SELECT d.id, d.tour,
       CONCAT(t.MaTour, '-', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL d.ngay DAY), '%y%m%d')),
       DATE_ADD(DATE_ADD(CURDATE(), INTERVAL d.ngay DAY), INTERVAL 7 HOUR),
       DATE_ADD(DATE_ADD(CURDATE(), INTERVAL d.ngay + t.SoNgay - 1 DAY), INTERVAL 17 HOUR),
       30,
       DATE_ADD(DATE_ADD(CURDATE(), INTERVAL d.ngay - 2 DAY), INTERVAL 7 HOUR),
       'MO_BAN'
FROM (SELECT 1 id, 1 tour, 14 ngay UNION ALL SELECT 2, 1, 28 UNION ALL
      SELECT 3, 2, 14 UNION ALL SELECT 4, 2, 28 UNION ALL
      SELECT 5, 3, 14 UNION ALL SELECT 6, 3, 28) d
JOIN Tour t ON t.TourID = d.tour;

INSERT INTO BangGiaDot (DotKhoiHanhID, LoaiKhachID, DonGia) VALUES
 (1,1,650000),(1,2,450000),(1,3,0),   (2,1,650000),(2,2,450000),(2,3,0),
 (3,1,1590000),(3,2,1190000),(3,3,0), (4,1,1590000),(4,2,1190000),(4,3,0),
 (5,1,1490000),(5,2,1090000),(5,3,0), (6,1,1490000),(6,2,1090000),(6,3,0);

/* Điểm đón: điểm của khu vực tour (không phụ thu) + TP.HCM (phụ thu 150.000đ, đón sớm 2 giờ) */
INSERT INTO DotKhoiHanh_DiemDon (DotKhoiHanhID, DiemDonID, GioDon, PhiPhuThuDon)
SELECT d.DotKhoiHanhID, d.TourID, d.NgayGioDi, 0 FROM DotKhoiHanh d
UNION ALL
SELECT d.DotKhoiHanhID, 4, DATE_SUB(d.NgayGioDi, INTERVAL 2 HOUR), 150000 FROM DotKhoiHanh d;

INSERT INTO TinTuc (TieuDe, Slug, TomTat, NoiDung, TrangThai, NgayDang) VALUES
 ('Khám phá tỉnh Vĩnh Long mới sau hợp nhất', 'kham-pha-tinh-vinh-long-moi',
  'Gợi ý các điểm đến tiêu biểu của ba khu vực Vĩnh Long, Bến Tre, Trà Vinh.',
  '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', NOW());
