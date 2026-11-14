package vn.edu.tvu.vinhlongtour.planner.scoring;

import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

/**
 * Tiêu chí độ nổi bật: admin chấm điểm đến từ 1 đến 5 sao, quy về 0–1.
 */
public record Popularity() implements ScoringCriterion {

    @Override
    public double weight() {
        return 0.3;
    }

    @Override
    public double value(Destination d) {
        return (d.getPopularity() - 1) / 4.0;
    }
}
