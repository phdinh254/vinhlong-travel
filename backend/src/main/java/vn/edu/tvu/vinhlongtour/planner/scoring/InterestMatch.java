package vn.edu.tvu.vinhlongtour.planner.scoring;

import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.util.Set;

/**
 * Tiêu chí sở thích: tỉ lệ loại hình của điểm đến nằm trong các loại hình khách chọn
 * (khách thích "Văn hóa", điểm vừa "Văn hóa" vừa "Lịch sử" được 0,5).
 */
public record InterestMatch(Set<Long> categoryIds) implements ScoringCriterion {

    @Override
    public double weight() {
        return 0.5;
    }

    @Override
    public double value(Destination d) {
        if (d.getCategories().isEmpty()) {
            return 0;
        }
        long matched = d.getCategories().stream().filter(c -> categoryIds.contains(c.getId())).count();
        return (double) matched / d.getCategories().size();
    }
}
