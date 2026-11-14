package vn.edu.tvu.vinhlongtour.planner.scoring;

import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;

import java.util.List;

/**
 * Một tiêu chí chấm điểm hấp dẫn của điểm đến với một khách cụ thể. Mỗi tiêu chí là một lớp riêng
 * (sở thích, độ nổi bật, khu vực ưu tiên); thêm tiêu chí mới chỉ cần thêm lớp, không sửa thuật toán.
 */
public interface ScoringCriterion {

    /** Trọng số của tiêu chí khi cộng chung. */
    double weight();

    /** Mức phù hợp của điểm đến theo tiêu chí này, từ 0 đến 1. */
    double value(Destination d);

    /** Điểm hấp dẫn S_i từ 1 đến 100: trung bình có trọng số của các tiêu chí. */
    static int score(List<ScoringCriterion> criteria, Destination d) {
        double weighted = criteria.stream().mapToDouble(c -> c.weight() * c.value(d)).sum();
        double total = criteria.stream().mapToDouble(ScoringCriterion::weight).sum();
        return (int) Math.max(1, Math.round(100 * weighted / total));
    }
}
