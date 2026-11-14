package vn.edu.tvu.vinhlongtour.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tvu.vinhlongtour.content.domain.Article;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleStatus;

import java.util.Optional;

/**
 * Truy vấn bảng bài viết; lọc động theo chuyên mục, trạng thái, từ khóa qua {@link JpaSpecificationExecutor}.
 */
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Tăng lượt xem ngay trong CSDL: nhiều người cùng đọc một bài không làm mất lượt của nhau. */
    @Modifying
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :id")
    void incrementViews(@Param("id") Long id);
}
