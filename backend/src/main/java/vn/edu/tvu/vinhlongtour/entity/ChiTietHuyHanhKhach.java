package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q36: hủy lẻ từng hành khách */
@Entity
@Table(name = "ChiTietHuyHanhKhach")
@Getter @Setter
public class ChiTietHuyHanhKhach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChiTietHuyID")
    private Integer chiTietHuyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "YeuCauHuyID")
    private YeuCauHuy yeuCauHuy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "HanhKhachID")
    private HanhKhach hanhKhach;

    @Column(name = "PhiPhat", nullable = false, precision = 18, scale = 0)
    private BigDecimal phiPhat = BigDecimal.ZERO;
}
