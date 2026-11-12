package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q07: bảng trung gian Tour_DichVu (BAO_GOM | KHONG_BAO_GOM) */
@Entity
@Table(name = "Tour_DichVu")
@Getter @Setter
public class TourDichVu {
    public static final String BAO_GOM = "BAO_GOM";
    public static final String KHONG_BAO_GOM = "KHONG_BAO_GOM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TourDichVuID")
    private Integer tourDichVuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @ManyToOne(optional = false)
    @JoinColumn(name = "DichVuID")
    private DichVu dichVu;

    @Column(name = "LoaiApDung", nullable = false, length = 20)
    private String loaiApDung;

    @Column(name = "GhiChu", length = 300)
    private String ghiChu;
}
