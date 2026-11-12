package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q21 */
@Entity
@Table(name = "PhuThu")
@Getter @Setter
public class PhuThu {
    public static final String THEO_NGUOI = "THEO_NGUOI";
    public static final String THEO_DON = "THEO_DON";
    public static final String THEO_SO_LUONG = "THEO_SO_LUONG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PhuThuID")
    private Integer phuThuId;

    /** null = áp dụng chung */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @Column(name = "TenPhuThu", nullable = false, length = 200)
    private String tenPhuThu;

    @Column(name = "CachTinh", nullable = false, length = 20)
    private String cachTinh;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGia;

    @Column(name = "ConHieuLuc", nullable = false)
    private Boolean conHieuLuc = true;
}
