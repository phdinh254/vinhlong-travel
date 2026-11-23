package vn.edu.tvu.vinhlongtour.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Lịch trình khách đã lưu từ chức năng gợi ý. Lưu nguyên lịch lúc gợi ý (dạng JSON) để admin sửa điểm đến
 * về sau thì lịch khách đã lưu vẫn giữ nguyên như lúc xem.
 */
@Entity
@Table(name = "saved_trips")
public class SavedTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String request;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String plan;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected SavedTrip() {
    }

    public SavedTrip(Account account, String title, String request, String plan, LocalDateTime createdAt) {
        this.account = account;
        this.title = title;
        this.request = request;
        this.plan = plan;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRequest() {
        return request;
    }

    public String getPlan() {
        return plan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
