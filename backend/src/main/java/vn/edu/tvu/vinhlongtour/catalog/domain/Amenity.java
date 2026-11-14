package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Dịch vụ có thể bao gồm hoặc không bao gồm trong tour: xe đưa đón, hướng dẫn viên, bữa trưa...
 */
@Entity
@Table(name = "amenities")
public class Amenity extends NamedItem {

    protected Amenity() {
    }

    public Amenity(String name) {
        super(name);
    }
}
