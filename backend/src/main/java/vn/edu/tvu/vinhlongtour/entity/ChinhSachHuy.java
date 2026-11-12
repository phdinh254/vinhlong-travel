package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Q37: chính sách hủy có phiên bản */
@Entity
@Table(name = "ChinhSachHuy")
@Getter @Setter
public class ChinhSachHuy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChinhSachHuyID")
    private Integer chinhSachHuyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @Column(name = "TenChinhSach", nullable = false, length = 200)
    private String tenChinhSach;

    @Column(name = "PhienBan", nullable = false)
    private Integer phienBan = 1;

    @Column(name = "TuNgay", nullable = false)
    private LocalDate tuNgay;

    @Column(name = "DenNgay")
    private LocalDate denNgay;

    @Column(name = "NoiDung", length = 100000)
    private String noiDung;

    @OneToMany(mappedBy = "chinhSachHuy", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("soNgayTruocTu DESC")
    private List<MocPhatHuy> mocPhats = new ArrayList<>();
}
