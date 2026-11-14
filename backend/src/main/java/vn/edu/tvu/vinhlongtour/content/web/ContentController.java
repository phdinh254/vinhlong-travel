package vn.edu.tvu.vinhlongtour.content.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleCategory;
import vn.edu.tvu.vinhlongtour.content.domain.SettingKey;
import vn.edu.tvu.vinhlongtour.content.service.ArticleService;
import vn.edu.tvu.vinhlongtour.content.service.ContactService;
import vn.edu.tvu.vinhlongtour.content.service.SiteSettingService;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleCard;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleDetail;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactReceived;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactRequest;

import java.util.Map;

/**
 * API công khai: đọc tin tức, đọc cấu hình trang (hotline, chính sách...), gửi tin nhắn liên hệ.
 */
@RestController
public class ContentController {

    private final ArticleService articles;
    private final SiteSettingService settings;
    private final ContactService contacts;

    public ContentController(ArticleService articles, SiteSettingService settings, ContactService contacts) {
        this.articles = articles;
        this.settings = settings;
        this.contacts = contacts;
    }

    @GetMapping("/api/v1/articles")
    public PageResponse<ArticleCard> articles(@RequestParam(required = false) ArticleCategory category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return articles.listPublished(category, page, size);
    }

    @GetMapping("/api/v1/articles/{slug}")
    public ArticleDetail article(@PathVariable String slug) {
        return articles.getPublished(slug);
    }

    @GetMapping("/api/v1/site-settings")
    public Map<SettingKey, String> siteSettings() {
        return settings.all();
    }

    @PostMapping("/api/v1/contact-messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactReceived contact(@Valid @RequestBody ContactRequest r) {
        return contacts.submit(r);
    }
}
