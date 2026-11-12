package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q16-Q17 */
@Entity
@Table(name = "BangGiaDot")
@Getter @Setter
public class BangGiaDot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BangGiaID")
    private Integer bangGiaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DotKhoiHanhID")
    private DotKhoiHanh dotKhoiHanh;

    @ManyToOne(optional = false)
    @JoinColumn(name = "LoaiKhachID")
    private LoaiKhach loaiKhach;

    @Column(name = "DonGia", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGia;
}
