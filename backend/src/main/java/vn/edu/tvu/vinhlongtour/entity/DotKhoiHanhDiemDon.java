package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Q20: điểm đón theo đợt, kèm giờ đón và phụ thu đón */
@Entity
@Table(name = "DotKhoiHanh_DiemDon")
@Getter @Setter
public class DotKhoiHanhDiemDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DotDiemDonID")
    private Integer dotDiemDonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DotKhoiHanhID")
    private DotKhoiHanh dotKhoiHanh;

    @ManyToOne(optional = false)
    @JoinColumn(name = "DiemDonID")
    private DiemDon diemDon;

    @Column(name = "GioDon", nullable = false)
    private LocalDateTime gioDon;

    @Column(name = "PhiPhuThuDon", nullable = false, precision = 18, scale = 0)
    private BigDecimal phiPhuThuDon = BigDecimal.ZERO;
}
