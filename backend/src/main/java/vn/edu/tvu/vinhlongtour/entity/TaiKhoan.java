package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Q22: tài khoản đăng nhập (LOCAL hoặc GOOGLE). */
@Entity
@Table(name = "TaiKhoan")
@Getter @Setter
public class TaiKhoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TaiKhoanID")
    private Integer taiKhoanId;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "MatKhauHash", length = 100)
    private String matKhauHash;

    @Column(name = "HoTen", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "AnhDaiDien", length = 500)
    private String anhDaiDien;

    @Column(name = "NhaCungCap", nullable = false, length = 20)
    private String nhaCungCap = "LOCAL";

    @Column(name = "GoogleSub", length = 100)
    private String googleSub;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "VaiTroID")
    private VaiTro vaiTro;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "HOAT_DONG";

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
