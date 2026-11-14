package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Điểm đón khách. Mỗi đợt khởi hành chọn vài điểm đón, kèm giờ đón và phí đón riêng (giai đoạn 3).
 * Điểm ngừng dùng thì tắt {@code active} thay vì xóa, để đợt cũ vẫn giữ được thông tin.
 */
@Entity
@Table(name = "pickup_points")
public class PickupPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    protected PickupPoint() {
    }

    public PickupPoint(String name, String address) {
        update(name, address, true);
    }

    public void update(String name, String address, boolean active) {
        this.name = name.trim();
        this.address = address == null || address.isBlank() ? null : address.trim();
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
