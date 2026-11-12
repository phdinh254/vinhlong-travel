package vn.edu.tvu.vinhlongtour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Nội dung trang Giới thiệu, thông tin liên hệ... dạng khóa-giá trị */
@Entity
@Table(name = "CauHinhTrang")
@Getter @Setter
public class CauHinhTrang {
    @Id
    @Column(name = "KhoaCauHinh", length = 50)
    private String khoaCauHinh;

    @Column(name = "GiaTri", length = 100000)
    private String giaTri;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime ngayCapNhat = LocalDateTime.now();
}
