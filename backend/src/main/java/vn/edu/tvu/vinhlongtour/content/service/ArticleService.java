package vn.edu.tvu.vinhlongtour.content.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.content.domain.Article;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleCategory;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleStatus;
import vn.edu.tvu.vinhlongtour.content.repository.ArticleRepository;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleCard;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleDetail;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiệp vụ bài viết: khách xem bài đã xuất bản (mỗi lần mở bài tăng lượt xem), admin soạn, sửa, xóa.
 */
@Service
@Transactional
public class ArticleService {

    private final ArticleRepository articles;
    private final Clock clock;

    public ArticleService(ArticleRepository articles, Clock clock) {
        this.articles = articles;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleCard> listPublished(ArticleCategory category, int page, int size) {
        return search(null, ArticleStatus.PUBLISHED, category, page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
    }

    public ArticleDetail getPublished(String slug) {
        Article a = articles.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED).orElseThrow(ArticleService::notFound);
        articles.incrementViews(a.getId());
        return ArticleDetail.from(a, a.getViewCount() + 1);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleCard> search(String q, ArticleStatus status, ArticleCategory category, int page, int size) {
        return search(q, status, category, page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
    }

    @Transactional(readOnly = true)
    public ArticleDetail get(Long id) {
        Article a = find(id);
        return ArticleDetail.from(a, a.getViewCount());
    }

    public ArticleDetail create(ArticleRequest r) {
        Article a = new Article(LocalDateTime.now(clock));
        apply(a, r, null);
        return ArticleDetail.from(articles.save(a), 0);
    }

    public ArticleDetail update(Long id, ArticleRequest r) {
        Article a = find(id);
        apply(a, r, a);
        return ArticleDetail.from(a, a.getViewCount());
    }

    public void delete(Long id) {
        articles.delete(find(id));
    }

    private void apply(Article a, ArticleRequest r, Article existing) {
        a.edit(r.title().trim(), slugFor(r, existing), blankToNull(r.summary()), r.content(), blankToNull(r.coverImageUrl()),
                r.category(), r.status(), LocalDateTime.now(clock));
    }

    /**
     * Admin tự đặt slug thì phải chưa ai dùng. Để trống: bài đang có giữ slug cũ (link đã chia sẻ không chết),
     * bài mới tạo từ tiêu đề, trùng thì thêm -2, -3...
     */
    private String slugFor(ArticleRequest r, Article existing) {
        Long id = existing == null ? null : existing.getId();
        if (r.slug() != null && !r.slug().isBlank()) {
            if (taken(r.slug(), id)) {
                throw ApiException.conflict("SLUG_TAKEN", "Đường dẫn \"" + r.slug() + "\" đã được bài khác dùng");
            }
            return r.slug();
        }
        if (existing != null) {
            return existing.getSlug();
        }
        String base = Slugs.of(r.title());
        String slug = base;
        for (int n = 2; taken(slug, null); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private boolean taken(String slug, Long id) {
        return id == null ? articles.existsBySlug(slug) : articles.existsBySlugAndIdNot(slug, id);
    }

    private PageResponse<ArticleCard> search(String q, ArticleStatus status, ArticleCategory category, int page, int size, Sort sort) {
        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (status != null) {
                where.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                where.add(cb.equal(root.get("category"), category));
            }
            if (q != null && !q.isBlank()) {
                where.add(cb.like(cb.lower(root.get("title")), "%" + q.trim().toLowerCase() + "%"));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        return PageResponse.of(articles.findAll(spec, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), sort))
                .map(ArticleCard::from));
    }

    private Article find(Long id) {
        return articles.findById(id).orElseThrow(ArticleService::notFound);
    }

    private static ApiException notFound() {
        return ApiException.notFound("NOT_FOUND", "Không tìm thấy bài viết");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
