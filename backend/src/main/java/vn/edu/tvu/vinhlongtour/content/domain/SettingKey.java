package vn.edu.tvu.vinhlongtour.content.domain;

/**
 * Các mục cấu hình trang web admin sửa được. Mục là đường dẫn ({@code link = true}) phải bắt đầu bằng
 * http:// hoặc https:// để không chèn được {@code javascript:} vào thẻ a/iframe của frontend.
 */
public enum SettingKey {
    /** Lời giới thiệu đơn vị (HTML), trang Giới thiệu. */
    ABOUT_HTML(false),
    HOTLINE(false),
    EMAIL(false),
    ADDRESS(false),
    /** Link nhúng Google Maps (embed URL) ở trang Liên hệ. */
    MAP_EMBED_URL(true),
    /** Giờ hỗ trợ hiện ở chân trang, ví dụ "Hỗ trợ 7:00 – 21:00". */
    SUPPORT_HOURS(false),
    FACEBOOK_URL(true),
    INSTAGRAM_URL(true),
    YOUTUBE_URL(true),
    TIKTOK_URL(true),
    /** Điều khoản đặt tour (HTML). */
    TERMS_HTML(false),
    /** Chính sách bảo mật (HTML). */
    PRIVACY_HTML(false),
    /** Trang chính sách hủy tour (HTML, phần giải thích; mức phạt thật lấy từ chính sách hủy đang áp dụng). */
    CANCELLATION_HTML(false);

    private final boolean link;

    SettingKey(boolean link) {
        this.link = link;
    }

    public boolean isLink() {
        return link;
    }
}
