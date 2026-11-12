package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q20 */
@Entity
@Table(name = "DiemDon")
@Getter @Setter
public class DiemDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiemDonID")
    private Integer diemDonId;

    @Column(name = "TenDiemDon", nullable = false, length = 200)
    private String tenDiemDon;

    @Column(name = "DiaChi", length = 300)
    private String diaChi;

    @ManyToOne
    @JoinColumn(name = "KhuVucID")
    private KhuVuc khuVuc;
}
