package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.TaiKhoan;

import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    Optional<TaiKhoan> findByEmailIgnoreCase(String email);
    Optional<TaiKhoan> findByGoogleSub(String googleSub);
    boolean existsByEmailIgnoreCase(String email);
}
