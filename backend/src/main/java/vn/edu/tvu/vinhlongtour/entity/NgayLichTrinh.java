package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Q08 + Q13: thuộc Tour (mẫu) HOẶC DotKhoiHanh (phiên bản theo đợt) */
@Entity
@Table(name = "NgayLichTrinh")
@Getter @Setter
public class NgayLichTrinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NgayLichTrinhID")
    private Integer ngayLichTrinhId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DotKhoiHanhID")
    private DotKhoiHanh dotKhoiHanh;

    @Column(name = "NgayThuMay", nullable = false)
    private Integer ngayThuMay;

    @Column(name = "TieuDeNgay", nullable = false, length = 250)
    private String tieuDeNgay;

    /** Q09 */
    @OneToMany(mappedBy = "ngayLichTrinh", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("thuTuHienThi ASC")
    private List<HoatDongLichTrinh> hoatDongs = new ArrayList<>();
}
