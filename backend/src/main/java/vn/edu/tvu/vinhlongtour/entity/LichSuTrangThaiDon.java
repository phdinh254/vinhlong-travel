package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Q30 */
@Entity
@Table(name = "LichSuTrangThaiDon")
@Getter @Setter
public class LichSuTrangThaiDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LichSuID")
    private Integer lichSuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DonDatTourID")
    private DonDatTour donDatTour;

    @Column(name = "TrangThaiCu", length = 20)
    private String trangThaiCu;

    @Column(name = "TrangThaiMoi", nullable = false, length = 20)
    private String trangThaiMoi;

    /** null = hệ thống tự động */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NguoiThayDoiID")
    private TaiKhoan nguoiThayDoi;

    @Column(name = "ThoiGian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();

    @Column(name = "LyDo", length = 500)
    private String lyDo;
}
