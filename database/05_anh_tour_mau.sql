/* Ảnh MẪU cho 3 tour mẫu (ảnh tĩnh trong frontend/public/images, nguồn: images/CREDITS.md).
   Thay bằng ảnh thật qua trang quản trị Tour, hoặc xóa: DELETE FROM HinhAnhTour WHERE DuongDan LIKE '/images/%'; */
USE vinhlongtour;

DELETE FROM HinhAnhTour WHERE DuongDan LIKE '/images/%';
INSERT INTO HinhAnhTour (TourID, DuongDan, LaAnhDaiDien, ThuTuHienThi)
SELECT t.TourID, x.DuongDan, x.LaAnhDaiDien, x.ThuTu
FROM Tour t
JOIN (
  SELECT 'VL-AB-1N' Ma, '/images/an-binh.jpg' DuongDan, 1 LaAnhDaiDien, 0 ThuTu UNION ALL
  SELECT 'VL-AB-1N', '/images/van-thanh-mieu.jpg', 0, 1 UNION ALL
  SELECT 'VL-AB-1N', '/images/lang-gom.jpg', 0, 2 UNION ALL
  SELECT 'BT-DUA-2N', '/images/con-phung.jpg', 1, 0 UNION ALL
  SELECT 'BT-DUA-2N', '/images/keo-dua.jpg', 0, 1 UNION ALL
  SELECT 'BT-DUA-2N', '/images/homestay.jpg', 0, 2 UNION ALL
  SELECT 'TV-KHMER-2N', '/images/chua-ang.jpg', 1, 0 UNION ALL
  SELECT 'TV-KHMER-2N', '/images/ao-ba-om.jpg', 0, 1 UNION ALL
  SELECT 'TV-KHMER-2N', '/images/chua-hang.jpg', 0, 2
) x ON x.Ma = t.MaTour;
