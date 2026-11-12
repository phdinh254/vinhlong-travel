package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Q23-Q24: hồ sơ người đặt/thanh toán. TaiKhoan = null với khách vãng lai. */
@Entity
@Table(name = "KhachHang")
@Getter @Setter
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KhachHangID")
    private Integer khachHangId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TaiKhoanID")
    private TaiKhoan taiKhoan;

    @Column(name = "HoTen", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "SoDienThoai", length = 20)
    private String soDienThoai;

    @Column(name = "Email", length = 150)
    private String email;

    @Column(name = "DiaChi", length = 300)
    private String diaChi;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
