package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.KhachHang;

import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    Optional<KhachHang> findByTaiKhoan_TaiKhoanId(Integer taiKhoanId);

    /** Q23: khách vãng lai đặt lại bằng cùng SĐT dùng chung hồ sơ để lưu lịch sử */
    Optional<KhachHang> findFirstBySoDienThoaiAndTaiKhoanIsNull(String soDienThoai);
}
