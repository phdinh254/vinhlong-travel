package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q03 */
@Entity
@Table(name = "KhuVuc")
@Getter @Setter
public class KhuVuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KhuVucID")
    private Integer khuVucId;

    @Column(name = "TenKhuVuc", nullable = false, unique = true, length = 150)
    private String tenKhuVuc;

    @Column(name = "MoTa", length = 500)
    private String moTa;
}
