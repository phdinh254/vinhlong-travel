package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chính sách hủy tour gồm nhiều mốc phạt, có hiệu lực từ ngày {@code effectiveFrom}.
 * Có phiên bản: muốn đổi chính sách thì tạo chính sách mới với ngày hiệu lực sau;
 * chính sách đã có hiệu lực thì không sửa/xóa được, vì đơn đặt tour đã chốt theo nó.
 */
@Entity
@Table(name = "cancellation_policies")
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "cancellation_policy_tiers", joinColumns = @JoinColumn(name = "policy_id"))
    private List<PenaltyTier> tiers = new ArrayList<>();

    protected CancellationPolicy() {
    }

    public CancellationPolicy(String name, LocalDate effectiveFrom, String description, List<PenaltyTier> tiers) {
        apply(name, effectiveFrom, description, tiers);
    }

    /** Sửa chính sách; chỉ được khi chính sách chưa có hiệu lực vào ngày {@code today}. */
    public void update(String name, LocalDate effectiveFrom, String description, List<PenaltyTier> tiers, LocalDate today) {
        ensureNotEffective(today);
        apply(name, effectiveFrom, description, tiers);
    }

    public void ensureNotEffective(LocalDate today) {
        if (isEffectiveOn(today)) {
            throw ApiException.conflict("POLICY_ALREADY_EFFECTIVE",
                    "Chính sách đã có hiệu lực nên không thể sửa hoặc xóa; hãy tạo chính sách mới");
        }
    }

    public boolean isEffectiveOn(LocalDate date) {
        return !effectiveFrom.isAfter(date);
    }

    /**
     * Phần trăm phạt khi hủy lúc còn {@code daysBefore} ngày trước khởi hành.
     * Không mốc nào khớp: đã qua ngày khởi hành thì phạt 100%, còn lại không phạt.
     */
    public int penaltyPercentFor(long daysBefore) {
        return tiers.stream()
                .filter(t -> t.covers(daysBefore))
                .mapToInt(PenaltyTier::penaltyPercent)
                .max()
                .orElse(daysBefore < 0 ? 100 : 0);
    }

    private void apply(String name, LocalDate effectiveFrom, String description, List<PenaltyTier> tiers) {
        for (int i = 0; i < tiers.size(); i++) {
            for (int j = i + 1; j < tiers.size(); j++) {
                if (tiers.get(i).overlaps(tiers.get(j))) {
                    throw ApiException.badRequest("INVALID_PENALTY_TIERS", "Các mốc phạt không được chồng nhau");
                }
            }
        }
        this.name = name.trim();
        this.effectiveFrom = effectiveFrom;
        this.description = description;
        this.tiers.clear();
        this.tiers.addAll(tiers);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getDescription() {
        return description;
    }

    /** Các mốc phạt, sắp theo số ngày tăng dần. */
    public List<PenaltyTier> getTiers() {
        return tiers.stream().sorted(Comparator.comparingInt(PenaltyTier::daysBeforeFrom)).toList();
    }
}
