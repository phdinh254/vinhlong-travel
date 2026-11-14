package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Loại hình du lịch (sinh thái, văn hóa, tâm linh...). Vừa dùng để phân loại tour,
 * vừa là "thẻ sở thích" của điểm đến để khớp với sở thích khách chọn khi gợi ý lịch trình.
 */
@Entity
@Table(name = "tour_categories")
public class TourCategory extends NamedItem {

    protected TourCategory() {
    }

    public TourCategory(String name) {
        super(name);
    }
}
