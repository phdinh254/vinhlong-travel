package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

/** Q01: thông tin cơ bản của tour (lịch trình mẫu: Q08). */
@Entity
@Table(name = "Tour")
@Getter @Setter
public class Tour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TourID")
    private Integer tourId;

    @Column(name = "MaTour", nullable = false, unique = true, length = 30)
    private String maTour;

    @Column(name = "TenTour", nullable = false, length = 250)
    private String tenTour;

    @Column(name = "MoTa", length = 100000)
    private String moTa;

    @Column(name = "ThoiLuong", nullable = false, length = 50)
    private String thoiLuong;

    @Column(name = "SoNgay", nullable = false)
    private Integer soNgay = 1;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai = "NHAP";

    @Column(name = "NoiBat", nullable = false)
    private Boolean noiBat = false;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "NgayCapNhat")
    private LocalDateTime ngayCapNhat;

    /** Q02 */
    @ManyToMany
    @JoinTable(name = "Tour_LoaiHinh",
            joinColumns = @JoinColumn(name = "TourID"),
            inverseJoinColumns = @JoinColumn(name = "LoaiHinhID"))
    private Set<LoaiHinh> loaiHinhs = new HashSet<>();

    /** Q03 */
    @ManyToMany
    @JoinTable(name = "Tour_KhuVuc",
            joinColumns = @JoinColumn(name = "TourID"),
            inverseJoinColumns = @JoinColumn(name = "KhuVucID"))
    private Set<KhuVuc> khuVucs = new HashSet<>();

    /** Q06 */
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("thuTuHienThi ASC")
    private List<HinhAnhTour> hinhAnhs = new ArrayList<>();

    /** Q07 */
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TourDichVu> dichVus = new ArrayList<>();

    /** Q08: lịch trình mẫu của tour */
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ngayThuMay ASC")
    private List<NgayLichTrinh> ngayLichTrinhs = new ArrayList<>();
}
