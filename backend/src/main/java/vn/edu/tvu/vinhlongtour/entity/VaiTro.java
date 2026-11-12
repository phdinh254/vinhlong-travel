package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "VaiTro")
@Getter @Setter
public class VaiTro {
    public static final String ADMIN = "ADMIN";
    public static final String KHACH_HANG = "KHACH_HANG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VaiTroID")
    private Integer vaiTroId;

    @Column(name = "MaVaiTro", nullable = false, unique = true, length = 30)
    private String maVaiTro;

    @Column(name = "TenVaiTro", nullable = false, length = 100)
    private String tenVaiTro;
}
