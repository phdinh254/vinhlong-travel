package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q02 */
@Entity
@Table(name = "LoaiHinh")
@Getter @Setter
public class LoaiHinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LoaiHinhID")
    private Integer loaiHinhId;

    @Column(name = "TenLoaiHinh", nullable = false, unique = true, length = 150)
    private String tenLoaiHinh;

    @Column(name = "MoTa", length = 500)
    private String moTa;
}
