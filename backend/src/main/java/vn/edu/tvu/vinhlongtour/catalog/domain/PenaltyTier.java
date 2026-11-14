package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Embeddable;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Một mốc phạt hủy tour: hủy khi còn từ {@code daysBeforeFrom} đến {@code daysBeforeTo} ngày
 * trước ngày khởi hành (tính cả hai đầu) thì bị phạt {@code penaltyPercent}% giá trị.
 */
@Embeddable
public record PenaltyTier(int daysBeforeFrom, int daysBeforeTo, int penaltyPercent) {

    public PenaltyTier {
        if (daysBeforeFrom < 0 || daysBeforeFrom > daysBeforeTo) {
            throw ApiException.badRequest("INVALID_PENALTY_TIERS", "Mốc ngày không hợp lệ: " + daysBeforeFrom + " đến " + daysBeforeTo);
        }
        if (penaltyPercent < 0 || penaltyPercent > 100) {
            throw ApiException.badRequest("INVALID_PENALTY_TIERS", "Phần trăm phạt phải từ 0 đến 100");
        }
    }

    public boolean covers(long daysBefore) {
        return daysBefore >= daysBeforeFrom && daysBefore <= daysBeforeTo;
    }

    public boolean overlaps(PenaltyTier other) {
        return daysBeforeFrom <= other.daysBeforeTo && other.daysBeforeFrom <= daysBeforeTo;
    }
}
