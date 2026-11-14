package vn.edu.tvu.vinhlongtour.content.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.content.domain.Article;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleCategory;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleStatus;
import vn.edu.tvu.vinhlongtour.content.domain.ContactMessage;
import vn.edu.tvu.vinhlongtour.content.domain.ContactStatus;

import java.time.LocalDateTime;

/**
 * Dữ liệu vào/ra của các API tin tức, cấu hình trang và liên hệ.
 */
public final class ContentDtos {

    private ContentDtos() {
    }

    // ---------- Request ----------

    /** {@code slug} để trống thì tự tạo từ tiêu đề. Nội dung là HTML do admin soạn. */
    public record ArticleRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 200) @Pattern(regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$", message = "chỉ gồm chữ thường không dấu, số và gạch ngang") String slug,
            @Size(max = 500) String summary,
            @NotBlank @Size(max = 200_000) String content,
            @Size(max = 500) @Pattern(regexp = "^$|^(https?://|/uploads/|/images/).+", message = "đường dẫn ảnh không hợp lệ") String coverImageUrl,
            @NotNull ArticleCategory category,
            @NotNull ArticleStatus status) {}

    /** Phải có số điện thoại hoặc email để gọi lại. */
    public record ContactRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = "^$|^[0-9+]{9,15}$", message = "số điện thoại không hợp lệ") String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 200) String subject,
            @NotBlank @Size(max = 3000) String message) {}

    public record ContactStatusRequest(@NotNull ContactStatus status) {}

    // ---------- Response ----------

    /** Một bài trong danh sách (không kèm nội dung). */
    public record ArticleCard(Long id, String title, String slug, String summary, String coverImageUrl, ArticleCategory category,
                              ArticleStatus status, long viewCount, LocalDateTime publishedAt, LocalDateTime updatedAt) {

        public static ArticleCard from(Article a) {
            return new ArticleCard(a.getId(), a.getTitle(), a.getSlug(), a.getSummary(), a.getCoverImageUrl(), a.getCategory(),
                    a.getStatus(), a.getViewCount(), a.getPublishedAt(), a.getUpdatedAt());
        }
    }

    public record ArticleDetail(Long id, String title, String slug, String summary, String content, String coverImageUrl,
                                ArticleCategory category, ArticleStatus status, long viewCount, LocalDateTime publishedAt,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {

        public static ArticleDetail from(Article a, long viewCount) {
            return new ArticleDetail(a.getId(), a.getTitle(), a.getSlug(), a.getSummary(), a.getContent(), a.getCoverImageUrl(),
                    a.getCategory(), a.getStatus(), viewCount, a.getPublishedAt(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record ContactMessageResponse(Long id, String fullName, String phone, String email, String subject, String message,
                                         ContactStatus status, LocalDateTime createdAt, LocalDateTime handledAt, String handledBy) {

        public static ContactMessageResponse from(ContactMessage m) {
            return new ContactMessageResponse(m.getId(), m.getFullName(), m.getPhone(), m.getEmail(), m.getSubject(), m.getMessage(),
                    m.getStatus(), m.getCreatedAt(), m.getHandledAt(), m.getHandledBy() == null ? null : m.getHandledBy().getFullName());
        }
    }

    /** Khách gửi xong chỉ cần biết đã nhận, không trả lại nội dung. */
    public record ContactReceived(Long id, LocalDateTime createdAt) {}
}
