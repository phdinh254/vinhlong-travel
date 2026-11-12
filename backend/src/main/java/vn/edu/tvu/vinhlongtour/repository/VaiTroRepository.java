package vn.edu.tvu.vinhlongtour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.entity.VaiTro;

import java.util.Optional;

public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {
    Optional<VaiTro> findByMaVaiTro(String maVaiTro);
}
