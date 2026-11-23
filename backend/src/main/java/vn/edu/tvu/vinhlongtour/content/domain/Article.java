package vn.edu.tvu.vinhlongtour.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bài viết tin tức/cẩm nang du lịch. Ngày xuất bản ghi ở lần xuất bản đầu tiên: ẩn bài rồi xuất bản lại
 * không làm bài cũ nhảy lên đầu danh sách.
 */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String content;

    @Column(length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status;

    @Column(nullable = false)
    private long viewCount;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Article() {
    }

    public Article(LocalDateTime now) {
        this.createdAt = now;
    }

    public void edit(String title, String slug, String summary, String content, String coverImageUrl,
                     ArticleCategory category, ArticleStatus status, LocalDateTime now) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.category = category;
        this.status = status;
        if (status == ArticleStatus.PUBLISHED && publishedAt == null) {
            this.publishedAt = now;
        }
        this.updatedAt = now;
    }

    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public ArticleCategory getCategory() {
        return category;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public long getViewCount() {
        return viewCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
