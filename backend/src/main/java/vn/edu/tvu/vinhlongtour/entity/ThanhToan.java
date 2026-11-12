package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Q32-Q34 */
@Entity
@Table(name = "ThanhToan")
@Getter @Setter
public class ThanhToan {
    public static final String CHO_THANH_TOAN = "CHO_THANH_TOAN";
    public static final String THANH_CONG = "THANH_CONG";
    public static final String THAT_BAI = "THAT_BAI";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ThanhToanID")
    private Integer thanhToanId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @Column(name = "SoTien", nullable = false, precision = 18, scale = 0)
    private BigDecimal soTien;

    /** COC | TAT_TOAN | TOAN_BO */
    @Column(name = "LoaiThanhToan", nullable = false, length = 20)
    private String loaiThanhToan;

    /** VNPAY | CHUYEN_KHOAN | TIEN_MAT */
    @Column(name = "PhuongThuc", nullable = false, length = 20)
    private String phuongThuc;

    @Column(name = "MaThamChieu", length = 50)
    private String maThamChieu;

    @Column(name = "MaGiaoDich", length = 50)
    private String maGiaoDich;

    @Column(name = "MaNganHang", length = 20)
    private String maNganHang;

    @Column(name = "ThoiGianTT")
    private LocalDateTime thoiGianTT;

    @Column(name = "AnhChungTu", length = 500)
    private String anhChungTu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiXacNhanID")
    private TaiKhoan nguoiXacNhan;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = CHO_THANH_TOAN;

    @Column(name = "DuLieuPhanHoi", length = 100000)
    private String duLieuPhanHoi;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
