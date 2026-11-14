package vn.edu.tvu.vinhlongtour.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repository chung cho các danh mục có tên không được trùng (không phân biệt hoa thường).
 * Các repository cụ thể kế thừa interface này để dùng chung phần kiểm tra trùng tên.
 */
@NoRepositoryBean
public interface UniqueNameRepository<T> extends JpaRepository<T, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
