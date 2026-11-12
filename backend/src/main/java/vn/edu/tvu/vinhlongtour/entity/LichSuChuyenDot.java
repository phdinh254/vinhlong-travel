package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Q39 */
@Entity
@Table(name = "LichSuChuyenDot")
@Getter @Setter
public class LichSuChuyenDot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChuyenDotID")
    private Integer chuyenDotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DotCuID")
    private DotKhoiHanh dotCu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DotMoiID")
    private DotKhoiHanh dotMoi;

    @Column(name = "LyDo", length = 500)
    private String lyDo;

    @Column(name = "ChenhLechTien", nullable = false, precision = 18, scale = 0)
    private BigDecimal chenhLechTien = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiThucHienID")
    private TaiKhoan nguoiThucHien;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();
}
