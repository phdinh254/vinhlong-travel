package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q21 */
@Entity
@Table(name = "ChiTietPhuThuDon")
@Getter @Setter
public class ChiTietPhuThuDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChiTietPhuThuID")
    private Integer chiTietPhuThuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PhuThuID")
    private PhuThu phuThu;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong = 1;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGia;

    @Column(name = "ThanhTien", nullable = false, precision = 18, scale = 0)
    private BigDecimal thanhTien;
}
