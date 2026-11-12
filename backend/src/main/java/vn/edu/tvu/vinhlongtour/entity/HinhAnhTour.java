package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Q06 */
@Entity
@Table(name = "HinhAnhTour")
@Getter @Setter
public class HinhAnhTour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HinhAnhID")
    private Integer hinhAnhId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TourID")
    private Tour tour;

    @Column(name = "DuongDan", nullable = false, length = 500)
    private String duongDan;

    @Column(name = "LaAnhDaiDien", nullable = false)
    private Boolean laAnhDaiDien = false;

    @Column(name = "ThuTuHienThi", nullable = false)
    private Integer thuTuHienThi = 0;
}
