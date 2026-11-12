package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Q35-Q36 */
@Entity
@Table(name = "YeuCauHuy")
@Getter @Setter
public class YeuCauHuy {
    public static final String CHO_XU_LY = "CHO_XU_LY";
    public static final String CHAP_NHAN = "CHAP_NHAN";
    public static final String TU_CHOI = "TU_CHOI";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "YeuCauHuyID")
    private Integer yeuCauHuyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    /** TOAN_BO | TUNG_PHAN */
    @Column(name = "LoaiHuy", nullable = false, length = 20)
    private String loaiHuy;

    @Column(name = "LyDo", length = 1000)
    private String lyDo;

    @Column(name = "ThoiGianYeuCau", nullable = false)
    private LocalDateTime thoiGianYeuCau = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiYeuCauID")
    private TaiKhoan nguoiYeuCau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiXuLyID")
    private TaiKhoan nguoiXuLy;

    @Column(name = "ThoiGianXuLy")
    private LocalDateTime thoiGianXuLy;

    @Column(name = "TongPhiPhat", nullable = false, precision = 18, scale = 0)
    private BigDecimal tongPhiPhat = BigDecimal.ZERO;

    @Column(name = "SoTienDuKienHoan", nullable = false, precision = 18, scale = 0)
    private BigDecimal soTienDuKienHoan = BigDecimal.ZERO;

    @Column(name = "KetQua", nullable = false, length = 20)
    private String ketQua = CHO_XU_LY;

    @Column(name = "GhiChuXuLy", length = 1000)
    private String ghiChuXuLy;

    @OneToMany(mappedBy = "yeuCauHuy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietHuyHanhKhach> chiTiets = new ArrayList<>();
}
