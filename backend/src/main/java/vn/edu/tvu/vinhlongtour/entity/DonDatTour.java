package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Q25, Q29, Q31, Q37 */
@Entity
@Table(name = "DonDatTour")
@Getter @Setter
public class DonDatTour {
    public static final String CHO_THANH_TOAN = "CHO_THANH_TOAN";
    public static final String DA_COC = "DA_COC";
    public static final String DA_THANH_TOAN = "DA_THANH_TOAN";
    public static final String HOAN_THANH = "HOAN_THANH";
    public static final String DA_HUY = "DA_HUY";
    public static final String HET_HAN = "HET_HAN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DonDatTourID")
    private Integer donDatTourId;

    @Column(name = "MaDon", nullable = false, unique = true, length = 30)
    private String maDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DotKhoiHanhID")
    private DotKhoiHanh dotKhoiHanh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "KhachHangID")
    private KhachHang khachHang;

    @Column(name = "TenNguoiDat", nullable = false, length = 150)
    private String tenNguoiDat;

    @Column(name = "SDTNguoiDat", nullable = false, length = 20)
    private String sdtNguoiDat;

    @Column(name = "EmailNguoiDat", length = 150)
    private String emailNguoiDat;

    @Column(name = "TongTienVe", nullable = false, precision = 18, scale = 0)
    private BigDecimal tongTienVe = BigDecimal.ZERO;

    @Column(name = "TongPhuThu", nullable = false, precision = 18, scale = 0)
    private BigDecimal tongPhuThu = BigDecimal.ZERO;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 0)
    private BigDecimal tongTien = BigDecimal.ZERO;

    @Column(name = "DaThanhToan", nullable = false, precision = 18, scale = 0)
    private BigDecimal daThanhToan = BigDecimal.ZERO;

    @Column(name = "SoChoChiem", nullable = false)
    private Integer soChoChiem = 0;

    @Column(name = "GhiChu", length = 1000)
    private String ghiChu;

    @Column(name = "NgayDat", nullable = false)
    private LocalDateTime ngayDat = LocalDateTime.now();

    @Column(name = "HanGiuCho")
    private LocalDateTime hanGiuCho;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = CHO_THANH_TOAN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChinhSachHuyID")
    private ChinhSachHuy chinhSachHuy;

    @OneToMany(mappedBy = "donDatTour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HanhKhach> hanhKhachs = new ArrayList<>();

    @OneToMany(mappedBy = "donDatTour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietPhuThuDon> phuThus = new ArrayList<>();

    public BigDecimal conLai() {
        return tongTien.subtract(daThanhToan).max(BigDecimal.ZERO);
    }
}
