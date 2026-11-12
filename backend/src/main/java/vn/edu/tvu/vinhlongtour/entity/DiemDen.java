package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Q04-Q05: điểm đến độc lập, liên kết tour qua HoatDongLichTrinh. */
@Entity
@Table(name = "DiemDen")
@Getter @Setter
public class DiemDen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiemDenID")
    private Integer diemDenId;

    @Column(name = "TenDiemDen", nullable = false, length = 200)
    private String tenDiemDen;

    @ManyToOne
    @JoinColumn(name = "KhuVucID")
    private KhuVuc khuVuc;

    @Column(name = "DiaChi", length = 300)
    private String diaChi;

    @Column(name = "ViDo", precision = 9, scale = 6)
    private BigDecimal viDo;

    @Column(name = "KinhDo", precision = 9, scale = 6)
    private BigDecimal kinhDo;

    @Column(name = "MoTa", length = 100000)
    private String moTa;

    @Column(name = "HinhAnh", length = 500)
    private String hinhAnh;
}
