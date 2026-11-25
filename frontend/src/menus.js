/**
 * Khai báo menu dùng chung cho toàn site (tương đương "Navigation Menu" của WordPress).
 * Header, footer và các trang khác đều đọc từ đây, nên chỉ cần sửa một chỗ.
 */

/** Menu chính (header) */
export const MAIN_MENU = [
  ['/', 'Trang chủ'],
  ['/gioi-thieu', 'Giới thiệu'],
  ['/tour', 'Tour du lịch'],
  ['/goi-y-lich-trinh', 'Gợi ý lịch trình'],
  ['/dich-vu', 'Dịch vụ'],
  ['/tin-tuc', 'Tin tức'],
  ['/lien-he', 'Liên hệ'],
]

/** Cột "Khám phá" ở footer */
export const FOOTER_EXPLORE_MENU = [
  ['/tour', 'Tour du lịch'],
  ['/goi-y-lich-trinh', 'Gợi ý lịch trình'],
  ['/dich-vu', 'Dịch vụ'],
  ['/gioi-thieu', 'Về chúng tôi'],
  ['/tin-tuc', 'Cẩm nang du lịch'],
  ['/tra-cuu', 'Tra cứu đơn đặt tour'],
  ['/lien-he', 'Gửi yêu cầu tư vấn'],
]

/**
 * Menu chính sách ở Footer Bottom.
 * Mỗi mục: [slug, tiêu đề, khóa cấu hình chứa nội dung HTML (admin soạn trong "Giới thiệu & thông tin")]
 */
export const POLICY_MENU = [
  ['dieu-khoan', 'Điều khoản đặt tour', 'TERMS_HTML'],
  ['bao-mat', 'Chính sách bảo mật', 'PRIVACY_HTML'],
  ['huy-tour', 'Chính sách hủy tour', 'CANCELLATION_HTML'],
]

/** Mạng xã hội: [khóa cấu hình, tên hiển thị] (icon nằm trong SiteFooter) */
export const SOCIAL_KEYS = [
  ['FACEBOOK', 'Facebook'],
  ['YOUTUBE', 'YouTube'],
  ['TIKTOK', 'TikTok'],
  ['INSTAGRAM', 'Instagram'],
]

/** Bỏ mọi ký tự không phải số trong số điện thoại (dùng cho tel: và Zalo) */
export const phoneDigits = (s) => (s || '').replace(/\D/g, '')
