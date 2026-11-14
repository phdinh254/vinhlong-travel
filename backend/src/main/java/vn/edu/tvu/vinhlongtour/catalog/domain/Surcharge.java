package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Khoản phụ thu khách có thể chọn thêm khi đặt tour (phòng đơn, bảo hiểm...).
 * Số tiền = đơn giá × số lượng, số lượng do {@link SurchargeMethod} quyết định.
 */
@Entity
@Table(name = "surcharges")
public class Surcharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurchargeMethod method;

    /** Đơn giá, đơn vị đồng. */
    @Column(nullable = false)
    private long unitPrice;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected Surcharge() {
    }

    public Surcharge(String name, SurchargeMethod method, long unitPrice, String description) {
        update(name, method, unitPrice, description, true);
    }

    public void update(String name, SurchargeMethod method, long unitPrice, String description, boolean active) {
        if (unitPrice < 0) {
            throw ApiException.badRequest("INVALID_PRICE", "Đơn giá không được âm");
        }
        this.name = name.trim();
        this.method = method;
        this.unitPrice = unitPrice;
        this.description = description;
        this.active = active;
    }

    /** Thành tiền của khoản phụ thu này cho một đơn. */
    public long amountFor(int passengers, Integer requestedUnits) {
        return unitPrice * method.quantity(passengers, requestedUnits);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SurchargeMethod getMethod() {
        return method;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
