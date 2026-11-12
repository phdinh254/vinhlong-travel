package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q07: danh mục dịch vụ */
@Entity
@Table(name = "DichVu")
@Getter @Setter
public class DichVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DichVuID")
    private Integer dichVuId;

    @Column(name = "TenDichVu", nullable = false, unique = true, length = 200)
    private String tenDichVu;

    @Column(name = "BieuTuong", length = 50)
    private String bieuTuong;
}
