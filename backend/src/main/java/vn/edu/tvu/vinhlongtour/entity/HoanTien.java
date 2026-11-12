package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Q38 */
@Entity
@Table(name = "HoanTien")
@Getter @Setter
public class HoanTien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HoanTienID")
    private Integer hoanTienId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "YeuCauHuyID")
    private YeuCauHuy yeuCauHuy;

    @Column(name = "SoTienHoan", nullable = false, precision = 18, scale = 0)
    private BigDecimal soTienHoan;

    @Column(name = "NgayHoan", nullable = false)
    private LocalDateTime ngayHoan = LocalDateTime.now();

    @Column(name = "PhuongThuc", nullable = false, length = 20)
    private String phuongThuc = "CHUYEN_KHOAN";

    @Column(name = "MaGiaoDichHoan", length = 50)
    private String maGiaoDichHoan;

    @Column(name = "AnhChungTu", length = 500)
    private String anhChungTu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiThucHienID")
    private TaiKhoan nguoiThucHien;

    @Column(name = "GhiChu", length = 500)
    private String ghiChu;
}
