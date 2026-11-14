package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;

/**
 * Giá vé của một loại khách trong một đợt khởi hành (đồng).
 */
@Embeddable
public class DeparturePrice {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_type_id")
    private PassengerType passengerType;

    @Column(nullable = false)
    private long price;

    protected DeparturePrice() {
    }

    public DeparturePrice(PassengerType passengerType, long price) {
        this.passengerType = passengerType;
        this.price = price;
    }

    public PassengerType getPassengerType() {
        return passengerType;
    }

    public long getPrice() {
        return price;
    }
}
