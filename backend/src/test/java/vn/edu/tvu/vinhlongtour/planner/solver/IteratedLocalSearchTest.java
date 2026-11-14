package vn.edu.tvu.vinhlongtour.planner.solver;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static vn.edu.tvu.vinhlongtour.planner.solver.TripProblem.LUNCH;

/**
 * Kiểm tra thuật toán bằng ví dụ tính tay trong luận văn Buijs (2015), mục 4.1–4.3: 4 vị trí, vị trí 0 là
 * điểm xuất phát, quỹ giờ 30. Các số mong đợi chép từ bảng 4, bảng 5 và các phép tính trong luận văn.
 * Bảng 3 của luận văn in nhầm (giờ về 22, MaxDelay 8) trong khi phép tính ngay sau dùng 20 và 10; test theo phép tính.
 */
class IteratedLocalSearchTest {

    /** Bảng 2: thời gian di chuyển giữa 4 vị trí. */
    private static final int[][] TRAVEL = {
            {0, 3, 4, 5},
            {3, 0, 5, 4},
            {4, 5, 0, 3},
            {5, 4, 3, 0}};

    // Bảng 1. Chỉ số trong danh sách = số hiệu vị trí − 1
    private static final int V1 = 0;
    private static final int V2 = 1;
    private static final int V3 = 2;

    private final TripProblem example = new TripProblem(TRAVEL, List.of(
            new Place(14, 7, 10, 25, 0, 1, Set.of()),
            new Place(4, 4, 4, 40, 0, 2, Set.of()),
            new Place(8, 6, 5, 20, 0, 3, Set.of())), null, 1, 0, 30, Long.MAX_VALUE);

    private final IteratedLocalSearch ils = new IteratedLocalSearch(example);

    @Test
    void tourWithVisitOneMatchesTheThesis() {
        Timeline t = example.schedule(List.of(V1), 0);

        assertThat(t.arrival()).containsExactly(3);
        assertThat(t.start()).containsExactly(10);
        assertThat(t.waiting()).containsExactly(7);
        assertThat(t.maxDelay()).containsExactly(10);
        assertThat(t.returnAt()).isEqualTo(20);
        assertThat(t.returnMaxDelay()).isEqualTo(10);
    }

    @Test
    void timeInsertionAndRatioMatchTheThesis() {
        List<List<Integer>> routes = List.of(List.of(V1));
        List<Timeline> timelines = List.of(example.schedule(routes.getFirst(), 0));

        var visit2 = ils.bestPosition(routes, timelines, V2);
        assertThat(visit2.timeInsertion()).isEqualTo(10);                        // 4 + 0 + 4 + 5 − 3
        assertThat(visit2.position()).isZero();                                  // hai vị trí bằng nhau, lấy vị trí đầu
        assertThat(visit2.ratio()).isCloseTo(1.6, within(1e-9));                 // 4² / 10

        var visit3 = ils.bestPosition(routes, timelines, V3);
        assertThat(visit3.timeInsertion()).isEqualTo(12);                        // 5 + 0 + 6 + 4 − 3
        assertThat(visit3.position()).isZero();                                  // vị trí sau visit 1 làm visit 3 trễ giờ
        assertThat(visit3.ratio()).isCloseTo(64.0 / 12, within(1e-9));           // 8² / 12 ≈ 5,3

        assertThat(ils.bestInsertion(routes.stream().map(ArrayList::new).collect(java.util.stream.Collectors.toList())).place())
                .isEqualTo(V3);
    }

    @Test
    void insertionStepGivesTable4() {
        List<List<Integer>> routes = new ArrayList<>(List.of(new ArrayList<>(List.of(V1))));

        ils.insertAll(routes);

        assertThat(routes.getFirst()).containsExactly(V3, V1);                   // 0 → 3 → 1 → 0, visit 2 không chèn thêm được
        Timeline t = example.schedule(routes.getFirst(), 0);
        assertThat(t.arrival()).containsExactly(5, 15);
        assertThat(t.start()).containsExactly(5, 15);
        assertThat(t.waiting()).containsExactly(0, 0);
        assertThat(t.maxDelay()).containsExactly(5, 5);
        assertThat(t.returnAt()).isEqualTo(25);
        assertThat(t.returnMaxDelay()).isEqualTo(5);
    }

    @Test
    void shakeStepGivesTable5() {
        List<List<Integer>> routes = new ArrayList<>(List.of(new ArrayList<>(List.of(V3, V1))));

        ils.shake(routes, 2, 1);                                                 // StartNumber = 2, RemoveNumber = 1

        assertThat(routes.getFirst()).containsExactly(V3);                       // 0 → 3 → 0
        Timeline t = example.schedule(routes.getFirst(), 0);
        assertThat(t.arrival()).containsExactly(5);
        assertThat(t.maxDelay()).containsExactly(14);
        assertThat(t.returnAt()).isEqualTo(16);
        assertThat(t.returnMaxDelay()).isEqualTo(14);
    }

    @Test
    void fullSearchFindsTheOptimumOfTheExample() {
        TripSolution s = ils.solve();

        // Ghé cả 3 điểm thì về muộn nhất là 31 > 30, nên 1 + 3 (điểm 22) là tốt nhất
        assertThat(s.score()).isEqualTo(22);
        assertThat(s.routes().getFirst()).containsExactlyInAnyOrder(V1, V3);
    }

    // ---------- Luật áp dụng cho Vĩnh Long: bữa trưa, ngân sách, ngày đóng cửa ----------

    /** 6 điểm quanh nơi lưu trú, đi lại 20 phút, mỗi điểm 90 phút, mở 7:00–17:00. */
    private static TripProblem sample(int days, long budget, Set<Integer> placeZeroClosedOn) {
        int[][] travel = new int[7][7];
        for (int a = 0; a < 7; a++) {
            for (int b = 0; b < 7; b++) {
                travel[a][b] = a == b ? 0 : 20;
            }
        }
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            places.add(new Place(10 + i * 10, 90, 7 * 60, 17 * 60 - 90, 100_000L * (i + 1), i + 1, i == 0 ? placeZeroClosedOn : Set.of()));
        }
        Place lunch = new Place(0, 60, 11 * 60, 13 * 60 + 30, 50_000, Place.HERE, Set.of());
        return new TripProblem(travel, places, lunch, days, 8 * 60, 16 * 60, budget);
    }

    @Test
    void everyDayHasLunchInsideLunchHoursAndEndsInTime() {
        TripProblem p = sample(2, Long.MAX_VALUE, Set.of());
        TripSolution s = new IteratedLocalSearch(p).solve();

        for (int d = 0; d < p.days(); d++) {
            List<Integer> route = s.routes().get(d);
            Timeline t = s.timelines().get(d);
            assertThat(t.feasible()).isTrue();
            assertThat(t.returnAt()).isLessThanOrEqualTo(16 * 60);
            int lunchAt = route.indexOf(LUNCH);
            assertThat(lunchAt).isNotNegative();
            assertThat(t.start()[lunchAt]).isBetween(11 * 60, 13 * 60 + 30);
        }
    }

    @Test
    void neverExceedsBudget() {
        TripProblem p = sample(2, 450_000, Set.of());                            // 2 bữa trưa 100.000 + tối đa 350.000 vé
        TripSolution s = new IteratedLocalSearch(p).solve();

        assertThat(s.cost()).isLessThanOrEqualTo(450_000);
        assertThat(s.routes().stream().flatMap(List::stream).filter(e -> e != LUNCH)).isNotEmpty();
    }

    @Test
    void skipsPlaceOnItsClosedDay() {
        TripProblem p = sample(1, Long.MAX_VALUE, Set.of(0));
        TripSolution s = new IteratedLocalSearch(p).solve();

        assertThat(s.routes().getFirst()).doesNotContain(0);
    }

    @Test
    void sameInputGivesSameResult() {
        TripProblem p = sample(2, 800_000, Set.of());

        assertThat(new IteratedLocalSearch(p).solve().routes()).isEqualTo(new IteratedLocalSearch(p).solve().routes());
    }
}
