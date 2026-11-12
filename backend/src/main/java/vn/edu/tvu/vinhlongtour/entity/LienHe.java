package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LienHe")
@Getter @Setter
public class LienHe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LienHeID")
    private Integer lienHeId;

    @Column(name = "HoTen", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "Email", nullable = false, length = 150)
    private String email;

    @Column(name = "SoDienThoai", length = 20)
    private String soDienThoai;

    @Column(name = "TieuDe", length = 250)
    private String tieuDe;

    @Column(name = "NoiDung", nullable = false, length = 2000)
    private String noiDung;

    /** MOI | DA_XU_LY */
    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "MOI";

    @Column(name = "NgayGui", nullable = false)
    private LocalDateTime ngayGui = LocalDateTime.now();
}
