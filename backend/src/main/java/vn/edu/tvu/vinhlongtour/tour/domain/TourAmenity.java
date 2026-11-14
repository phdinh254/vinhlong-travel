package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import vn.edu.tvu.vinhlongtour.catalog.domain.Amenity;

/**
 * Một dịch vụ của tour và việc nó có nằm trong giá hay không
 * (trang chi tiết tour hiện hai cột "Bao gồm" / "Không bao gồm").
 */
@Embeddable
public class TourAmenity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;

    @Column(nullable = false)
    private boolean included;

    protected TourAmenity() {
    }

    public TourAmenity(Amenity amenity, boolean included) {
        this.amenity = amenity;
        this.included = included;
    }

    public Amenity getAmenity() {
        return amenity;
    }

    public boolean isIncluded() {
        return included;
    }
}
