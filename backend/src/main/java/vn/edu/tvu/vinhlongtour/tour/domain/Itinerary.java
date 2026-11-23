package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lịch trình nhiều ngày, mỗi ngày gồm các hoạt động theo giờ.
 * Tour giữ một lịch trình mẫu; mỗi đợt khởi hành giữ bản sao riêng tạo bằng {@link #copy()}
 * (mẫu thiết kế Prototype), nên sửa lịch trình của một đợt không ảnh hưởng tour hay đợt khác.
 */
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber")
    private List<ItineraryDay> days = new ArrayList<>();

    public Itinerary() {
    }

    /**
     * Thay toàn bộ lịch trình; ngày được đánh số 1, 2, 3... theo thứ tự truyền vào.
     * Ngày đã có thì sửa tại chỗ, chỉ thêm/xóa ngày ở cuối: CSDL có unique (lịch trình, số ngày) mà Hibernate
     * chèn dòng mới trước khi xóa dòng cũ, nên xóa hết rồi tạo lại sẽ vướng trùng số ngày.
     */
    public void replaceDays(List<DayPlan> plans) {
        for (int i = 0; i < plans.size(); i++) {
            if (i < days.size()) {
                days.get(i).apply(plans.get(i));
            } else {
                days.add(new ItineraryDay(this, i + 1, plans.get(i)));
            }
        }
        while (days.size() > plans.size()) {
            days.removeLast();
        }
    }

    /** Bản sao sâu: ngày và hoạt động mới, cùng tham chiếu tới các điểm đến cũ. */
    public Itinerary copy() {
        Itinerary copy = new Itinerary();
        copy.replaceDays(days.stream().map(ItineraryDay::toPlan).toList());
        return copy;
    }

    public int dayCount() {
        return days.size();
    }

    public Long getId() {
        return id;
    }

    public List<ItineraryDay> getDays() {
        return List.copyOf(days);
    }

    /** Dữ liệu một ngày khi tạo/sửa lịch trình. */
    public record DayPlan(String title, String description, List<ActivityPlan> activities) {}

    /** Dữ liệu một hoạt động; {@code startTime} và {@code destination} có thể để trống. */
    public record ActivityPlan(LocalTime startTime, String title, String description, Destination destination) {}
}
