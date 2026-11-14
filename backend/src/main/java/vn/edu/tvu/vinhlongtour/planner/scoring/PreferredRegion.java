package vn.edu.tvu.vinhlongtour.planner.scoring;

import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.util.Set;

/**
 * Tiêu chí khu vực ưu tiên: điểm đến thuộc khu vực khách muốn đi (Vĩnh Long, Bến Tre, Trà Vinh cũ) được 1, còn lại 0.
 */
public record PreferredRegion(Set<Long> regionIds) implements ScoringCriterion {

    @Override
    public double weight() {
        return 0.2;
    }

    @Override
    public double value(Destination d) {
        return regionIds.contains(d.getRegion().getId()) ? 1 : 0;
    }
}
