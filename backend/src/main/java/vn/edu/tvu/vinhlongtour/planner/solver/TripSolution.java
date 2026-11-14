package vn.edu.tvu.vinhlongtour.planner.solver;

import java.util.List;

/**
 * Kết quả giải: mỗi ngày một tuyến (chỉ số điểm trong {@link TripProblem#places()}, bữa trưa là
 * {@link TripProblem#LUNCH}) kèm lịch chi tiết, tổng điểm hấp dẫn và tổng chi phí.
 */
public record TripSolution(List<List<Integer>> routes, List<Timeline> timelines, int score, long cost) {
}
