/* Thêm chuyên mục cho bài viết (trang Tin tức kiểu tạp chí).
   Chạy một lần trên database đã tạo từ 01_schema.sql bản cũ. An toàn khi chạy lại. */
USE vinhlongtour;

ALTER TABLE TinTuc
  ADD COLUMN IF NOT EXISTS ChuyenMuc VARCHAR(20) NOT NULL DEFAULT 'TIN_TUC' AFTER TomTat;  -- TIN_TUC | VAN_HOA | SU_KIEN | KINH_NGHIEM
