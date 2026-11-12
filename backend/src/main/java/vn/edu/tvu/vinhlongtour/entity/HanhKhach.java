package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Q26-Q28: người đi thực tế */
@Entity
@Table(name = "HanhKhach")
@Getter @Setter
public class HanhKhach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HanhKhachID")
    private Integer hanhKhachId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @Column(name = "HoTen", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "NgaySinh", nullable = false)
    private LocalDate ngaySinh;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh;

    @Column(name = "SoDienThoai", length = 20)
    private String soDienThoai;

    @Column(name = "CCCD_Passport", length = 30)
    private String cccdPassport;

    @ManyToOne(optional = false)
    @JoinColumn(name = "LoaiKhachID")
    private LoaiKhach loaiKhach;

    @ManyToOne
    @JoinColumn(name = "DiemDonID")
    private DiemDon diemDon;

    /** Q28: đơn giá chốt tại thời điểm đặt */
    @Column(name = "GiaApDung", nullable = false, precision = 18, scale = 0)
    private BigDecimal giaApDung;

    @Column(name = "PhiDon", nullable = false, precision = 18, scale = 0)
    private BigDecimal phiDon = BigDecimal.ZERO;

    /** HIEU_LUC | DA_HUY */
    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HIEU_LUC";
}
