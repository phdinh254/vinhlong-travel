package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/** Q09-Q12 */
@Entity
@Table(name = "HoatDongLichTrinh")
@Getter @Setter
public class HoatDongLichTrinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HoatDongID")
    private Integer hoatDongId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "NgayLichTrinhID")
    private NgayLichTrinh ngayLichTrinh;

    /** DI_CHUYEN | AN_UONG | THAM_QUAN | NGHI_NGOI | KHAC */
    @Column(name = "LoaiHoatDong", nullable = false, length = 20)
    private String loaiHoatDong;

    @Column(name = "GioBatDau")
    private LocalTime gioBatDau;

    @Column(name = "GioKetThuc")
    private LocalTime gioKetThuc;

    @Column(name = "NoiDung", nullable = false, length = 1000)
    private String noiDung;

    @Column(name = "ThuTuHienThi", nullable = false)
    private Integer thuTuHienThi = 0;

    /** Q11-Q12: optional */
    @ManyToOne
    @JoinColumn(name = "DiemDenID")
    private DiemDen diemDen;
}
