package vn.edu.tvu.vinhlongtour.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.catalog.domain.SurchargeMethod;

/**
 * Một khoản phụ thu khách đã chọn trong đơn. Tên, cách tính và đơn giá được chụp lại lúc đặt,
 * để đổi giá phụ thu sau này không làm thay đổi đơn cũ.
 */
@Embeddable
public class BookingSurcharge {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "surcharge_id")
    private Surcharge surcharge;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurchargeMethod method;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private long amount;

    protected BookingSurcharge() {
    }

    public BookingSurcharge(Surcharge surcharge, int quantity) {
        this.surcharge = surcharge;
        this.name = surcharge.getName();
        this.method = surcharge.getMethod();
        this.quantity = quantity;
        this.unitPrice = surcharge.getUnitPrice();
        this.amount = unitPrice * quantity;
    }

    public Surcharge getSurcharge() {
        return surcharge;
    }

    public String getName() {
        return name;
    }

    public SurchargeMethod getMethod() {
        return method;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public long getAmount() {
        return amount;
    }
}
