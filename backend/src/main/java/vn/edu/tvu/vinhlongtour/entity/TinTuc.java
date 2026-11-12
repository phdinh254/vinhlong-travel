package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TinTuc")
@Getter @Setter
public class TinTuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TinTucID")
    private Integer tinTucId;

    @Column(name = "TieuDe", nullable = false, length = 300)
    private String tieuDe;

    @Column(name = "Slug", nullable = false, unique = true, length = 320)
    private String slug;

    @Column(name = "TomTat", length = 600)
    private String tomTat;

    @Column(name = "NoiDung", nullable = false, length = 100000)
    private String noiDung;

    @Column(name = "AnhBia", length = 500)
    private String anhBia;

    /** NHAP | XUAT_BAN */
    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "NHAP";

    @Column(name = "LuotXem", nullable = false)
    private Integer luotXem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TacGiaID")
    private TaiKhoan tacGia;

    @Column(name = "NgayDang")
    private LocalDateTime ngayDang;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
