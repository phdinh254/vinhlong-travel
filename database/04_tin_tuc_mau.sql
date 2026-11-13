/* Bài viết MẪU để xem trước trang Tin tức. Thay bằng nội dung thật hoặc xóa:
   DELETE FROM TinTuc WHERE Slug LIKE 'mau-%';
   Ảnh bìa để trống: giao diện tự dùng ảnh minh họa theo chuyên mục. */
USE vinhlongtour;

INSERT IGNORE INTO TinTuc (TieuDe, Slug, TomTat, ChuyenMuc, NoiDung, TrangThai, LuotXem, NgayDang) VALUES
('Một ngày trên cù lao An Bình: xuồng ba lá, vườn trái và bữa cơm quê', 'mau-mot-ngay-cu-lao-an-binh',
 'Rời bến phà, chỉ mười phút đò ngang là bước vào một thế giới khác: rạch nhỏ, vườn chôm chôm và những ngôi nhà mở cửa đón khách.',
 'KINH_NGHIEM', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 214, NOW() - INTERVAL 1 DAY),
('Mùa Ok Om Bok: đêm trăng rằm tháng Mười của người Khmer Trà Vinh', 'mau-mua-ok-om-bok',
 'Lễ cúng trăng, thả đèn nước và những chiếc ghe ngo rẽ sóng: mùa lễ hội đẹp nhất trong năm của đồng bào Khmer.',
 'VAN_HOA', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 389, NOW() - INTERVAL 2 DAY),
('Lịch tour cuối năm: thêm khởi hành cuối tuần đến Cồn Phụng', 'mau-lich-tour-cuoi-nam',
 'Đáp ứng nhu cầu mùa cao điểm, các tour xứ dừa 2 ngày 1 đêm có thêm lịch khởi hành thứ Bảy hằng tuần.',
 'TIN_TUC', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 96, NOW() - INTERVAL 3 DAY),
('Làng gạch gốm Mang Thít – “vương quốc đỏ” bên dòng Cổ Chiên', 'mau-lang-gom-mang-thit',
 'Hàng trăm lò gạch hình vòm nối nhau dọc kênh Thầy Cai, kể câu chuyện về một nghề đã nuôi sống bao thế hệ.',
 'VAN_HOA', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 172, NOW() - INTERVAL 5 DAY),
('Chôl Chnăm Thmây: Tết cổ truyền của đồng bào Khmer', 'mau-chol-chnam-thmay',
 'Giữa tháng Tư dương lịch, chùa Khmer rộn ràng lễ rước, tắm Phật và đắp núi cát cầu phúc.',
 'SU_KIEN', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 143, NOW() - INTERVAL 7 DAY),
('Ăn gì ở xứ dừa? 7 món nên thử khi đến Bến Tre', 'mau-an-gi-o-xu-dua',
 'Từ kẹo dừa, bánh xèo bột gạo đến đuông dừa: hành trình ẩm thực quanh một loại cây.',
 'KINH_NGHIEM', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 251, NOW() - INTERVAL 9 DAY),
('Văn Thánh Miếu Vĩnh Long – nơi lưu giữ tinh thần hiếu học Nam Bộ', 'mau-van-thanh-mieu',
 'Một trong số ít văn miếu ở miền Nam, công trình là điểm hẹn của những ai muốn hiểu chiều sâu văn hóa vùng đất.',
 'VAN_HOA', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 88, NOW() - INTERVAL 12 DAY),
('Kinh nghiệm đi Ao Bà Om và chùa Âng trong một buổi sáng', 'mau-ao-ba-om-chua-ang',
 'Hai điểm đến nằm sát nhau ở Trà Vinh: nên đi giờ nào, mặc gì khi vào chùa và ăn sáng ở đâu.',
 'KINH_NGHIEM', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 120, NOW() - INTERVAL 14 DAY),
('Ra mắt tuyến tour mới khám phá biển Ba Động', 'mau-tour-bien-ba-dong',
 'Hành trình kết hợp tham quan rừng ngập mặn, thưởng thức hải sản và ngắm bình minh trên biển Trà Vinh.',
 'TIN_TUC', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 64, NOW() - INTERVAL 16 DAY),
('Đua ghe ngo – nhịp trống trên sông của người Khmer', 'mau-dua-ghe-ngo',
 'Những chiếc ghe dài hàng chục mét, vài chục tay chèo đồng loạt: môn thể thao gắn liền với lễ hội Ok Om Bok.',
 'SU_KIEN', '<p>Bài viết mẫu – thay bằng nội dung thật.</p>', 'XUAT_BAN', 77, NOW() - INTERVAL 20 DAY);

/* Gán chuyên mục cho bài mẫu có sẵn từ 02_seed.sql */
UPDATE TinTuc SET ChuyenMuc = 'TIN_TUC' WHERE Slug = 'kham-pha-tinh-vinh-long-moi';

/* Ảnh bìa minh họa cho bài mẫu (ảnh tĩnh trong frontend/public/images) */
UPDATE TinTuc SET AnhBia = '/images/an-binh.jpg'        WHERE Slug = 'mau-mot-ngay-cu-lao-an-binh';
UPDATE TinTuc SET AnhBia = '/images/chua-hang.jpg'      WHERE Slug = 'mau-mua-ok-om-bok';
UPDATE TinTuc SET AnhBia = '/images/con-phung.jpg'      WHERE Slug = 'mau-lich-tour-cuoi-nam';
UPDATE TinTuc SET AnhBia = '/images/lang-gom.jpg'       WHERE Slug = 'mau-lang-gom-mang-thit';
UPDATE TinTuc SET AnhBia = '/images/chua-ang.jpg'       WHERE Slug = 'mau-chol-chnam-thmay';
UPDATE TinTuc SET AnhBia = '/images/keo-dua.jpg'        WHERE Slug = 'mau-an-gi-o-xu-dua';
UPDATE TinTuc SET AnhBia = '/images/van-thanh-mieu.jpg' WHERE Slug = 'mau-van-thanh-mieu';
UPDATE TinTuc SET AnhBia = '/images/ao-ba-om.jpg'       WHERE Slug = 'mau-ao-ba-om-chua-ang';
UPDATE TinTuc SET AnhBia = '/images/song-nuoc.jpg'      WHERE Slug = 'mau-tour-bien-ba-dong';
UPDATE TinTuc SET AnhBia = '/images/vui-choi.jpg'       WHERE Slug = 'mau-dua-ghe-ngo';
UPDATE TinTuc SET AnhBia = '/images/mang-thit.jpg'      WHERE Slug = 'kham-pha-tinh-vinh-long-moi' AND AnhBia IS NULL;
