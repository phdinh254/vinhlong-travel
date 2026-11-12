package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q37: mốc phạt theo số ngày hủy trước ngày khởi hành */
@Entity
@Table(name = "MocPhatHuy")
@Getter @Setter
public class MocPhatHuy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MocPhatID")
    private Integer mocPhatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChinhSachHuyID")
    private ChinhSachHuy chinhSachHuy;

    @Column(name = "SoNgayTruocTu", nullable = false)
    private Integer soNgayTruocTu;

    @Column(name = "SoNgayTruocDen", nullable = false)
    private Integer soNgayTruocDen;

    @Column(name = "PhanTramPhat", nullable = false, precision = 5, scale = 2)
    private BigDecimal phanTramPhat;
}
