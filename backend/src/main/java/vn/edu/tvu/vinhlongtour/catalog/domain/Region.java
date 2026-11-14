package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Khu vực trong tỉnh Vĩnh Long mới, theo tỉnh cũ: Vĩnh Long, Bến Tre, Trà Vinh.
 * Dùng để lọc tour và điểm đến, và để gom điểm gần nhau khi gợi ý lịch trình.
 */
@Entity
@Table(name = "regions")
public class Region extends NamedItem {

    protected Region() {
    }

    public Region(String name) {
        super(name);
    }
}
