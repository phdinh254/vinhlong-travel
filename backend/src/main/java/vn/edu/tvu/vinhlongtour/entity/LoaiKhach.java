package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q16, Q18, Q19 */
@Entity
@Table(name = "LoaiKhach")
@Getter @Setter
public class LoaiKhach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LoaiKhachID")
    private Integer loaiKhachId;

    @Column(name = "TenLoaiKhach", nullable = false, unique = true, length = 100)
    private String tenLoaiKhach;

    @Column(name = "TuoiTu", nullable = false)
    private Integer tuoiTu;

    @Column(name = "TuoiDen", nullable = false)
    private Integer tuoiDen;

    /** 1: ngồi ghế riêng, 0: ngồi chung (không trừ sức chứa) */
    @Column(name = "HeSoChiemCho", nullable = false)
    private Integer heSoChiemCho = 1;
}
