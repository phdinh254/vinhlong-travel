package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.LienHe;

import java.util.List;

public interface LienHeRepository extends JpaRepository<LienHe, Integer> {
    List<LienHe> findAllByOrderByNgayGuiDesc();
    long countByTrangThai(String trangThai);
}
