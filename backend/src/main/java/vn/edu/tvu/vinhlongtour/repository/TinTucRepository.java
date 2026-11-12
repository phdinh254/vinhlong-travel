package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.TinTuc;

import java.util.Optional;

public interface TinTucRepository extends JpaRepository<TinTuc, Integer> {
    Page<TinTuc> findByTrangThaiOrderByNgayDangDesc(String trangThai, Pageable pageable);
    Optional<TinTuc> findBySlugAndTrangThai(String slug, String trangThai);
    boolean existsBySlug(String slug);
}
