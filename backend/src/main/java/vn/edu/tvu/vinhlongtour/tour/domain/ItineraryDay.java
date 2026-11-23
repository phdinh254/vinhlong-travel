package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một ngày trong lịch trình: tiêu đề ngày và các hoạt động theo thứ tự.
 * Chỉ được tạo qua {@link Itinerary#replaceDays(List)}.
 */
@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @Column(nullable = false)
    private int dayNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ItineraryActivity> activities = new ArrayList<>();

    protected ItineraryDay() {
    }

    ItineraryDay(Itinerary itinerary, int dayNumber, Itinerary.DayPlan plan) {
        this.itinerary = itinerary;
        this.dayNumber = dayNumber;
        apply(plan);
    }

    /** Ghi đè tiêu đề, mô tả và toàn bộ hoạt động của ngày này, giữ nguyên số ngày. */
    void apply(Itinerary.DayPlan plan) {
        this.title = plan.title().trim();
        this.description = plan.description();
        activities.clear();
        LocalTime previous = null;
        for (int i = 0; i < plan.activities().size(); i++) {
            Itinerary.ActivityPlan a = plan.activities().get(i);
            if (a.startTime() != null) {
                if (previous != null && a.startTime().isBefore(previous)) {
                    throw ApiException.badRequest("INVALID_ITINERARY",
                            "Ngày " + dayNumber + ": giờ bắt đầu các hoạt động phải theo thứ tự tăng dần");
                }
                previous = a.startTime();
            }
            activities.add(new ItineraryActivity(this, i + 1, a));
        }
    }

    Itinerary.DayPlan toPlan() {
        return new Itinerary.DayPlan(title, description, activities.stream().map(ItineraryActivity::toPlan).toList());
    }

    public Long getId() {
        return id;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<ItineraryActivity> getActivities() {
        return List.copyOf(activities);
    }
}
