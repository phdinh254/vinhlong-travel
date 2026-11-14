package vn.edu.tvu.vinhlongtour.planner.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;
import vn.edu.tvu.vinhlongtour.planner.domain.LodgingPreset;
import vn.edu.tvu.vinhlongtour.planner.domain.Pace;
import vn.edu.tvu.vinhlongtour.planner.solver.IteratedLocalSearch;
import vn.edu.tvu.vinhlongtour.planner.solver.Place;
import vn.edu.tvu.vinhlongtour.planner.solver.TripProblem;
import vn.edu.tvu.vinhlongtour.planner.solver.TripSolution;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SuggestRequest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.tvu.vinhlongtour.planner.solver.TripProblem.LUNCH;

/**
 * So sánh ILS với cách tham lam đơn giản (luôn chọn điểm hấp dẫn nhất còn xếp được, nối vào cuối ngày)
 * trên 38 điểm đến thật, nhiều nơi lưu trú, số ngày và nhịp đi khác nhau. In bảng kết quả để đưa vào báo cáo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IlsVersusGreedyTests {

    @Autowired
    TripPlannerService service;

    @Test
    @Transactional(readOnly = true)
    void ilsNeverScoresBelowGreedy() {
        LocalDate start = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        StringBuilder table = new StringBuilder("""
                | Nơi lưu trú | Số ngày | Nhịp đi | Tham lam | Chỉ bước chèn | ILS | ILS hơn tham lam | Thời gian ILS (ms) |
                |---|---|---|---|---|---|---|---|
                """);
        int greedyTotal = 0;
        int ilsTotal = 0;
        for (LodgingPreset lodging : LodgingPreset.values()) {
            for (int days = 1; days <= 3; days++) {
                for (Pace pace : List.of(Pace.RELAXED, Pace.PACKED)) {
                    SuggestRequest r = new SuggestRequest(start, days, pace, lodging, null, null, 2, 0, null, null, null);
                    TripProblem problem = service.prepare(r).problem();
                    int greedy = greedy(problem);
                    int insertion = new IteratedLocalSearch(problem).insertionOnly().score();
                    long t0 = System.nanoTime();
                    TripSolution ils = new IteratedLocalSearch(problem).solve();
                    long ms = (System.nanoTime() - t0) / 1_000_000;

                    assertThat(ils.score()).as("%s %d ngày %s", lodging, days, pace).isGreaterThanOrEqualTo(greedy);
                    assertThat(ils.timelines()).allMatch(t -> t.feasible());
                    greedyTotal += greedy;
                    ilsTotal += ils.score();
                    table.append("| %s | %d | %s | %d | %d | %d | %+.1f%% | %d |%n".formatted(lodging.displayName(), days, pace,
                            greedy, insertion, ils.score(), 100.0 * (ils.score() - greedy) / greedy, ms));
                }
            }
        }
        table.append("%nTổng điểm: tham lam %d, ILS %d (%+.1f%%)%n".formatted(greedyTotal, ilsTotal, 100.0 * (ilsTotal - greedyTotal) / greedyTotal));
        System.out.println(table);
    }

    /** Mỗi ngày lần lượt nối vào cuối ngày điểm có điểm hấp dẫn cao nhất mà vẫn xếp được và không vượt ngân sách. */
    private static int greedy(TripProblem p) {
        List<Integer> byScore = IntStream.range(0, p.places().size()).boxed()
                .sorted(Comparator.comparingInt((Integer i) -> -p.places().get(i).score())).toList();
        Set<Integer> used = new HashSet<>();
        long cost = (long) p.days() * p.lunch().cost();
        int score = 0;
        for (int day = 0; day < p.days(); day++) {
            List<Integer> route = new ArrayList<>(List.of(LUNCH));
            boolean added = true;
            while (added) {
                added = false;
                for (int j : byScore) {
                    Place place = p.places().get(j);
                    if (used.contains(j) || cost + place.cost() > p.budget()) {
                        continue;
                    }
                    // Trước bữa trưa nếu chưa tới giờ ăn, sau bữa trưa nếu đã ăn: nối vào cuối phần tương ứng
                    List<Integer> tried = tryAppend(p, route, j, day);
                    if (tried != null) {
                        route = tried;
                        used.add(j);
                        cost += place.cost();
                        score += place.score();
                        added = true;
                        break;
                    }
                }
            }
        }
        return score;
    }

    /** Nối điểm vào cuối ngày; nếu trễ giờ ăn trưa thì thử đặt ngay trước bữa trưa (bữa trưa luôn ở cuối buổi sáng). */
    private static List<Integer> tryAppend(TripProblem p, List<Integer> route, int j, int day) {
        List<Integer> atEnd = new ArrayList<>(route);
        atEnd.add(j);
        if (p.schedule(atEnd, day).feasible()) {
            return atEnd;
        }
        List<Integer> beforeLunch = new ArrayList<>(route);
        beforeLunch.add(route.indexOf(LUNCH), j);
        return p.schedule(beforeLunch, day).feasible() ? beforeLunch : null;
    }
}
