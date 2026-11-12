package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Q14-Q15: đợt khởi hành của tour */
@Entity
@Table(name = "DotKhoiHanh")
@Getter @Setter
public class DotKhoiHanh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DotKhoiHanhID")
    private Integer dotKhoiHanhId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @Column(name = "MaDot", nullable = false, unique = true, length = 40)
    private String maDot;

    @Column(name = "NgayGioDi", nullable = false)
    private LocalDateTime ngayGioDi;

    @Column(name = "NgayGioVe", nullable = false)
    private LocalDateTime ngayGioVe;

    @Column(name = "SucChuaToiDa", nullable = false)
    private Integer sucChuaToiDa;

    @Column(name = "SoChoDaDat", nullable = false)
    private Integer soChoDaDat = 0;

    @Column(name = "HanNhanKhach", nullable = false)
    private LocalDateTime hanNhanKhach;

    /** MO_BAN | HET_CHO | DONG | DA_KHOI_HANH | HUY */
    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "MO_BAN";

    /** Q16-Q17 */
    @OneToMany(mappedBy = "dotKhoiHanh", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BangGiaDot> bangGias = new ArrayList<>();

    /** Q20 */
    @OneToMany(mappedBy = "dotKhoiHanh", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DotKhoiHanhDiemDon> diemDons = new ArrayList<>();

    /** Q13: lịch trình riêng của đợt (sao chép từ lịch trình mẫu) */
    @OneToMany(mappedBy = "dotKhoiHanh", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ngayThuMay ASC")
    private List<NgayLichTrinh> ngayLichTrinhs = new ArrayList<>();

    public int soChoConLai() {
        return sucChuaToiDa - soChoDaDat;
    }
}
