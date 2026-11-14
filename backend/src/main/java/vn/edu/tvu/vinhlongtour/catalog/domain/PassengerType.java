package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Loại khách theo độ tuổi (người lớn, trẻ em, em bé). Mỗi đợt khởi hành đặt giá riêng cho từng loại;
 * tuổi của hành khách tính đến ngày khởi hành để chọn loại.
 * {@code seats} là số chỗ chiếm trên xe (em bé ngồi lòng người lớn: 0).
 */
@Entity
@Table(name = "passenger_types")
public class PassengerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Embedded
    private AgeRange ageRange;

    @Column(nullable = false)
    private int seats;

    protected PassengerType() {
    }

    public PassengerType(String name, AgeRange ageRange, int seats) {
        update(name, ageRange, seats);
    }

    public void update(String name, AgeRange ageRange, int seats) {
        if (seats < 0 || seats > 2) {
            throw ApiException.badRequest("INVALID_SEATS", "Số chỗ chiếm phải từ 0 đến 2");
        }
        this.name = name.trim();
        this.ageRange = ageRange;
        this.seats = seats;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public int getSeats() {
        return seats;
    }
}
