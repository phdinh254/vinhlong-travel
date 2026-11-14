package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import vn.edu.tvu.vinhlongtour.catalog.domain.PickupPoint;

import java.time.LocalTime;

/**
 * Một điểm đón của đợt khởi hành: giờ đón và phí đón tính trên mỗi hành khách (đồng).
 */
@Embeddable
public class DeparturePickup {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_point_id")
    private PickupPoint pickupPoint;

    @Column(nullable = false)
    private LocalTime pickupTime;

    @Column(nullable = false)
    private long fee;

    protected DeparturePickup() {
    }

    public DeparturePickup(PickupPoint pickupPoint, LocalTime pickupTime, long fee) {
        this.pickupPoint = pickupPoint;
        this.pickupTime = pickupTime;
        this.fee = fee;
    }

    public PickupPoint getPickupPoint() {
        return pickupPoint;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public long getFee() {
        return fee;
    }
}
