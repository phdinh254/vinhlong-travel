/**
 * Nội dung tĩnh của trang chủ. Sửa chữ ở đây, không cần đụng giao diện (Home.jsx).
 * `diemDen` / `loaiHinh` được so khớp theo TÊN với danh mục trong database để tạo link lọc tour.
 */

/** Điểm đến nổi bật (ô đầu tiên là ô lớn của lưới bento) */
export const DIEM_DEN = [
  { k: 'an-binh', ten: 'Cù lao An Bình', noi: 'Vĩnh Long', loai: 'Sinh thái miệt vườn', diemDen: 'Cù lao An Bình' },
  { k: 'con-phung', ten: 'Cồn Phụng', noi: 'Bến Tre', loai: 'Cồn – xứ dừa', diemDen: 'Cồn Phụng' },
  { k: 'ao-ba-om', ten: 'Ao Bà Om', noi: 'Trà Vinh', loai: 'Thắng cảnh', diemDen: 'Ao Bà Om' },
  { k: 'van-thanh-mieu', ten: 'Văn Thánh Miếu', noi: 'Vĩnh Long', loai: 'Di tích lịch sử', diemDen: 'Văn Thánh Miếu' },
  { k: 'mang-thit', ten: 'Làng gốm Mang Thít', noi: 'Vĩnh Long', loai: 'Làng nghề', diemDen: 'Mang Thít' },
]

/** Trải nghiệm miền Tây: mỗi thẻ dẫn tới các tour có hoạt động tương ứng */
export const TRAI_NGHIEM = [
  { k: 'kenh-rach-2', ten: 'Xuồng ba lá qua kênh rạch', mo: 'Len lỏi dưới tán dừa nước, nghe tiếng mái chèo khua.', loaiHinh: 'Sinh thái' },
  { k: 'homestay', ten: 'Ngủ nhà vườn trên cù lao', mo: 'Cùng chủ nhà nấu món quê, hái trái, trò chuyện bên hiên.', loaiHinh: 'Homestay' },
  { k: 'keo-dua', ten: 'Ẩm thực xứ dừa', mo: 'Xem làm kẹo dừa, thưởng thức món ngon từ dừa Bến Tre.', loaiHinh: 'Ẩm thực' },
  { k: 'chua-ang', ten: 'Văn hóa Khmer', mo: 'Chùa cổ, kiến trúc tháp nhọn và nhịp sống của đồng bào Khmer.', diemDen: 'Chùa Âng' },
  { k: 'lang-gom', ten: 'Làng nghề gạch gốm', mo: 'Đi giữa “vương quốc đỏ” với hàng trăm lò gạch hình vòm.', loaiHinh: 'Làng nghề' },
  { k: 'van-thanh-mieu', ten: 'Di tích & lịch sử', mo: 'Văn Thánh Miếu, khu di tích Đồng Khởi và chuyện đất học.', loaiHinh: 'Văn hóa' },
]

/** Quy trình đặt tour */
export const QUY_TRINH = [
  ['Chọn tour', 'Tìm theo điểm đến, khu vực, loại hình, thời lượng và xem lịch khởi hành.'],
  ['Đặt tour', 'Chọn đợt khởi hành, điểm đón, nhập thông tin hành khách.'],
  ['Thanh toán & xác nhận', 'Đặt cọc hoặc thanh toán qua VNPay; nhân viên liên hệ xác nhận.'],
  ['Trải nghiệm', 'Lên xe tại điểm đón và bắt đầu hành trình khám phá miền Tây.'],
]

/** SỐ LIỆU MINH HỌA – thay bằng số liệu thật trước khi vận hành chính thức */
export const SO_LIEU = [
  ['12.000+', 'Lượt khách'],
  ['4.9/5', 'Điểm đánh giá'],
  ['98%', 'Muốn quay lại'],
]

/** ĐÁNH GIÁ MINH HỌA – thay bằng phản hồi thật (có sự đồng ý của khách) */
export const DANH_GIA = [
  { ten: 'Ngọc Anh', noi: 'TP. Hồ Chí Minh', sao: 5, tour: 'Cù lao An Bình 1 ngày', thang: 'Tháng 10/2026', nd: 'Đi xuồng qua rạch dừa nước, ăn trái cây tại vườn. Hướng dẫn viên rất nhiệt tình, kể chuyện vui.' },
  { ten: 'Minh Khoa', noi: 'Cần Thơ', sao: 5, tour: 'Xứ dừa Bến Tre 2N1Đ', thang: 'Tháng 9/2026', nd: 'Đặt tour nhanh, thanh toán VNPay tiện. Lịch trình đúng như mô tả, homestay sạch sẽ.' },
  { ten: 'Thu Hà', noi: 'Hà Nội', sao: 5, tour: 'Văn hóa Khmer Trà Vinh 2N1Đ', thang: 'Tháng 9/2026', nd: 'Chùa Khmer ở Trà Vinh đẹp ngoài sức tưởng tượng. Sẽ quay lại cùng cả gia đình.' },
  { ten: 'Quốc Bảo', noi: 'Bình Dương', sao: 4, tour: 'Xứ dừa Bến Tre 2N1Đ', thang: 'Tháng 8/2026', nd: 'Món ăn ngon, chủ nhà dễ thương. Mong có thêm thời gian tự do ở cồn.' },
  { ten: 'Lan Chi', noi: 'Đà Nẵng', sao: 5, tour: 'Cù lao An Bình 1 ngày', thang: 'Tháng 7/2026', nd: 'Lần đầu xuống miền Tây, mọi thứ chậm rãi và bình yên. Rất đáng đi một lần.' },
]

/** Câu hỏi thường gặp */
export const FAQ = [
  ['Làm thế nào để đặt tour?',
    'Chọn tour ở trang Tour du lịch, bấm “Đặt tour” tại đợt khởi hành phù hợp, chọn điểm đón và nhập thông tin hành khách. Sau khi đặt, bạn nhận được mã đơn để theo dõi và thanh toán.'],
  ['Tôi có cần tạo tài khoản không?',
    'Không bắt buộc. Bạn có thể đặt tour và tra cứu đơn bằng mã đơn và số điện thoại ở mục “Tra cứu đơn”. Tài khoản giúp xem lại lịch sử đặt tour thuận tiện hơn.'],
  ['Có hỗ trợ thanh toán trực tuyến không?',
    'Có. Bạn có thể đặt cọc hoặc thanh toán toàn bộ qua cổng VNPay ngay trên trang đơn đặt tour.'],
  ['Có thể hủy hoặc thay đổi tour không?',
    'Bạn có thể gửi yêu cầu hủy trên trang đơn đặt tour. Phí hủy tính theo số ngày trước khởi hành, được ghi rõ trong mục “Chính sách hủy” của từng tour.'],
  ['Giá tour đã bao gồm những gì?',
    'Mỗi tour có mục “Bao gồm” và “Không bao gồm” trong trang chi tiết. Các khoản phụ thu (phòng đơn, điểm đón xa…) được hiển thị trước khi bạn xác nhận đặt.'],
  ['Có tour riêng cho nhóm, gia đình hoặc công ty không?',
    'Có. Hãy gửi yêu cầu ở trang Liên hệ với số khách và ngày dự kiến, chúng tôi sẽ thiết kế lịch trình và báo giá riêng.'],
]
