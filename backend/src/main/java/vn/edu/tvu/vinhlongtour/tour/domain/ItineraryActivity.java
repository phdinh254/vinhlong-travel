package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.time.LocalTime;

/**
 * Một hoạt động trong ngày, ví dụ "08:00 – Tham quan Văn Thánh Miếu".
 * Có thể gắn với một điểm đến; gợi ý lịch trình dùng liên kết này để so lịch trình của khách với tour có sẵn.
 */
@Entity
@Table(name = "itinerary_activities")
public class ItineraryActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id")
    private ItineraryDay day;

    @Column(nullable = false)
    private int sortOrder;

    private LocalTime startTime;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    protected ItineraryActivity() {
    }

    ItineraryActivity(ItineraryDay day, int sortOrder, Itinerary.ActivityPlan plan) {
        this.day = day;
        this.sortOrder = sortOrder;
        this.startTime = plan.startTime();
        this.title = plan.title().trim();
        this.description = plan.description();
        this.destination = plan.destination();
    }

    Itinerary.ActivityPlan toPlan() {
        return new Itinerary.ActivityPlan(startTime, title, description, destination);
    }

    public Long getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Destination getDestination() {
        return destination;
    }
}
