package vn.edu.tvu.vinhlongtour.content.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleCategory;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleStatus;
import vn.edu.tvu.vinhlongtour.content.domain.ContactStatus;
import vn.edu.tvu.vinhlongtour.content.domain.SettingKey;
import vn.edu.tvu.vinhlongtour.content.service.ArticleService;
import vn.edu.tvu.vinhlongtour.content.service.ContactService;
import vn.edu.tvu.vinhlongtour.content.service.SiteSettingService;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleCard;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleDetail;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleRequest;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactMessageResponse;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactStatusRequest;

import java.util.Map;

/**
 * API quản trị nội dung: soạn bài viết, sửa cấu hình trang, xử lý tin nhắn liên hệ. Chỉ ADMIN gọi được.
 */
@RestController
public class AdminContentController {

    private final ArticleService articles;
    private final SiteSettingService settings;
    private final ContactService contacts;

    public AdminContentController(ArticleService articles, SiteSettingService settings, ContactService contacts) {
        this.articles = articles;
        this.settings = settings;
        this.contacts = contacts;
    }

    @GetMapping("/api/v1/admin/articles")
    public PageResponse<ArticleCard> articles(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) ArticleStatus status,
                                              @RequestParam(required = false) ArticleCategory category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return articles.search(q, status, category, page, size);
    }

    @GetMapping("/api/v1/admin/articles/{id}")
    public ArticleDetail article(@PathVariable Long id) {
        return articles.get(id);
    }

    @PostMapping("/api/v1/admin/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDetail create(@Valid @RequestBody ArticleRequest r) {
        return articles.create(r);
    }

    @PutMapping("/api/v1/admin/articles/{id}")
    public ArticleDetail update(@PathVariable Long id, @Valid @RequestBody ArticleRequest r) {
        return articles.update(id, r);
    }

    @DeleteMapping("/api/v1/admin/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        articles.delete(id);
    }

    /** Gửi các mục cần đổi, ví dụ {@code {"HOTLINE": "0270 3 822 222"}}; trả về toàn bộ cấu hình sau khi lưu. */
    @PutMapping("/api/v1/admin/site-settings")
    public Map<SettingKey, String> updateSettings(@RequestBody @NotNull Map<SettingKey, String> changes) {
        return settings.update(changes);
    }

    @GetMapping("/api/v1/admin/contact-messages")
    public PageResponse<ContactMessageResponse> contacts(@RequestParam(required = false) ContactStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return contacts.list(status, page, size);
    }

    @PatchMapping("/api/v1/admin/contact-messages/{id}/status")
    public ContactMessageResponse changeContactStatus(Authentication auth, @PathVariable Long id,
                                                      @Valid @RequestBody ContactStatusRequest r) {
        return contacts.changeStatus(id, r.status(), Long.valueOf(auth.getName()));
    }
}
