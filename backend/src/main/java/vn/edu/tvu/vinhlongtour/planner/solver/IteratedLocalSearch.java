package vn.edu.tvu.vinhlongtour.planner.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static vn.edu.tvu.vinhlongtour.planner.solver.TripProblem.LUNCH;

/**
 * Giải TOPTW bằng Iterated Local Search (Vansteenwegen và cộng sự, 2009; cài đặt theo luận văn Buijs, 2015):
 * lặp bước "chèn" điểm có Ratio = S² / TimeInsertion lớn nhất cho tới khi hết chèn được, rồi bước "lắc" bỏ bớt
 * vài điểm liên tiếp mỗi ngày để thoát lời giải kẹt. Dừng sau 150 lần liên tiếp không tìm được tổng điểm cao hơn.
 * Không dùng số ngẫu nhiên nên cùng đầu vào luôn cho cùng kết quả.
 * <p>
 * Khác bài báo: kiểm tra chèn được hay không bằng cách tính lại lịch cả ngày thay vì cập nhật dần Wait/MaxDelay.
 * Vì bữa trưa diễn ra "tại chỗ" nên chèn một điểm ngay trước bữa trưa đổi luôn vị trí ăn trưa, công thức cập nhật
 * dần không còn đúng; còn tính lại thì với vài chục điểm vẫn chỉ mất vài chục mili-giây.
 */
public final class IteratedLocalSearch {

    static final int MAX_NO_IMPROVEMENT = 150;

    private final TripProblem problem;

    public IteratedLocalSearch(TripProblem problem) {
        this.problem = problem;
    }

    /** Một cách chèn: điểm {@code place} vào ngày {@code day} tại vị trí {@code position}. */
    record Insertion(int place, int day, int position, int timeInsertion, double ratio) {}

    public TripSolution solve() {
        List<List<Integer>> routes = emptyRoutes();
        List<List<Integer>> best = copy(routes);
        int bestScore = -1;
        int startNumber = 1;
        int removeNumber = 1;
        int noImprovement = 0;
        int resetRemoveAt = Math.max(1, problem.places().size() / (3 * problem.days()));
        while (noImprovement < MAX_NO_IMPROVEMENT) {
            insertAll(routes);
            int score = score(routes);
            if (score > bestScore) {
                best = copy(routes);
                bestScore = score;
                removeNumber = 1;
                noImprovement = 0;
            } else {
                noImprovement++;
            }
            int smallest = routes.stream().mapToInt(r -> visitCount(r)).min().orElse(0);
            shake(routes, startNumber, removeNumber);
            startNumber += removeNumber;
            removeNumber++;
            if (smallest > 0 && startNumber >= smallest) {
                startNumber -= smallest;
            }
            if (removeNumber >= resetRemoveAt) {
                removeNumber = 1;
            }
        }
        return toSolution(best);
    }

    /** Lời giải chỉ dùng bước chèn một lần (không lắc), để so sánh với ILS đầy đủ. */
    public TripSolution insertionOnly() {
        List<List<Integer>> routes = emptyRoutes();
        insertAll(routes);
        return toSolution(routes);
    }

    /** Bước chèn: chèn lần lượt điểm có Ratio lớn nhất cho tới khi không chèn thêm được. */
    void insertAll(List<List<Integer>> routes) {
        Insertion best;
        while ((best = bestInsertion(routes)) != null) {
            routes.get(best.day()).add(best.position(), best.place());
        }
    }

    Insertion bestInsertion(List<List<Integer>> routes) {
        List<Timeline> timelines = timelines(routes);
        Set<Integer> used = used(routes);
        long cost = cost(routes);
        Insertion best = null;
        for (int j = 0; j < problem.places().size(); j++) {
            Place p = problem.places().get(j);
            if (used.contains(j) || p.score() <= 0 || cost + p.cost() > problem.budget()) {
                continue;
            }
            Insertion candidate = bestPosition(routes, timelines, j);
            if (candidate != null && (best == null || candidate.ratio() > best.ratio())) {
                best = candidate;
            }
        }
        return best;
    }

    /** Vị trí khả thi có TimeInsertion nhỏ nhất của điểm {@code j} (bằng nhau thì lấy vị trí đứng trước). */
    Insertion bestPosition(List<List<Integer>> routes, List<Timeline> timelines, int j) {
        Place p = problem.places().get(j);
        int[][] c = problem.travel();
        Insertion best = null;
        for (int day = 0; day < routes.size(); day++) {
            if (p.closedOn(day)) {
                continue;
            }
            List<Integer> route = routes.get(day);
            Timeline t = timelines.get(day);
            for (int pos = 0; pos <= route.size(); pos++) {
                int i = problem.locationBefore(route, pos);
                int k = problem.locationAfter(route, pos);
                int leave = pos == 0 ? problem.dayStart() : t.start()[pos - 1] + problem.node(route.get(pos - 1)).serviceMinutes();
                int arrival = leave + c[i][p.location()];
                int wait = Math.max(0, p.open() - arrival);
                if (arrival + wait > p.latestStart()) {
                    continue;
                }
                // TimeInsertion_j = c_ij + Wait_j + T_j + c_jk − c_ik
                int timeInsertion = c[i][p.location()] + wait + p.serviceMinutes() + c[p.location()][k] - c[i][k];
                if (best != null && timeInsertion >= best.timeInsertion()) {
                    continue;
                }
                List<Integer> tried = new ArrayList<>(route);
                tried.add(pos, j);
                if (problem.schedule(tried, day).feasible()) {
                    // Đường làm tròn lên từng chặng nên đi vòng đôi khi rẻ hơn đi thẳng 1 phút; chặn chia cho số ≤ 0
                    double ratio = (double) p.score() * p.score() / Math.max(1, timeInsertion);
                    best = new Insertion(j, day, pos, timeInsertion, ratio);
                }
            }
        }
        return best;
    }

    /**
     * Bước lắc: mỗi ngày bỏ {@code removeNumber} điểm liên tiếp, bắt đầu từ điểm thứ {@code startNumber}
     * (đếm từ 1, không tính bữa trưa), hết ngày thì quay lại đầu ngày. Các điểm sau tự dồn lên sớm hơn
     * vì lịch luôn được tính lại theo kiểu bắt đầu sớm nhất.
     */
    void shake(List<List<Integer>> routes, int startNumber, int removeNumber) {
        for (List<Integer> route : routes) {
            List<Integer> visits = route.stream().filter(e -> e != LUNCH).toList();
            if (visits.isEmpty()) {
                continue;
            }
            Set<Integer> removed = new HashSet<>();
            for (int k = 0; k < Math.min(removeNumber, visits.size()); k++) {
                // Cách cập nhật StartNumber của bài báo có thể cho ra 0, hiểu là điểm cuối ngày
                removed.add(visits.get(Math.floorMod(startNumber - 1 + k, visits.size())));
            }
            route.removeIf(removed::contains);
        }
    }

    private List<List<Integer>> emptyRoutes() {
        List<List<Integer>> routes = new ArrayList<>();
        for (int d = 0; d < problem.days(); d++) {
            routes.add(problem.lunch() == null ? new ArrayList<>() : new ArrayList<>(List.of(LUNCH)));
        }
        return routes;
    }

    private TripSolution toSolution(List<List<Integer>> routes) {
        return new TripSolution(routes.stream().map(List::copyOf).toList(), timelines(routes), score(routes), cost(routes));
    }

    private List<Timeline> timelines(List<List<Integer>> routes) {
        List<Timeline> result = new ArrayList<>();
        for (int d = 0; d < routes.size(); d++) {
            result.add(problem.schedule(routes.get(d), d));
        }
        return result;
    }

    private int score(List<List<Integer>> routes) {
        return routes.stream().flatMap(List::stream).mapToInt(e -> problem.node(e).score()).sum();
    }

    private long cost(List<List<Integer>> routes) {
        return routes.stream().flatMap(List::stream).mapToLong(e -> problem.node(e).cost()).sum();
    }

    private static Set<Integer> used(List<List<Integer>> routes) {
        Set<Integer> used = new HashSet<>();
        routes.forEach(used::addAll);
        return used;
    }

    private static int visitCount(List<Integer> route) {
        return (int) route.stream().filter(e -> e != LUNCH).count();
    }

    private static List<List<Integer>> copy(List<List<Integer>> routes) {
        List<List<Integer>> copy = new ArrayList<>();
        routes.forEach(r -> copy.add(new ArrayList<>(r)));
        return copy;
    }
}
