package vn.edu.tvu.vinhlongtour.planner.solver;

import java.util.List;

/**
 * Dữ liệu vào của bài toán TOPTW (Team Orienteering Problem with Time Windows): các điểm có thể ghé,
 * bảng thời gian di chuyển giữa các vị trí (vị trí 0 là nơi lưu trú, mỗi ngày đi và về đây), số ngày,
 * quỹ giờ mỗi ngày, bữa trưa bắt buộc mỗi ngày ({@code null} nếu không cần) và ngân sách cả chuyến.
 * Một tuyến (một ngày) là danh sách chỉ số trong {@code places}, bữa trưa ghi là {@link #LUNCH}.
 */
public record TripProblem(int[][] travel, List<Place> places, Place lunch, int days, int dayStart, int dayEnd, long budget) {

    /** Mã của bữa trưa trong một tuyến. */
    public static final int LUNCH = -1;

    public Place node(int entry) {
        return entry == LUNCH ? lunch : places.get(entry);
    }

    /** Tính lịch của một ngày theo kiểu "bắt đầu sớm nhất có thể": tới sớm thì chờ tới giờ mở cửa. */
    public Timeline schedule(List<Integer> route, int day) {
        int n = route.size();
        int[] travelTo = new int[n];
        int[] arrival = new int[n];
        int[] start = new int[n];
        int[] wait = new int[n];
        int[] maxDelay = new int[n];
        int time = dayStart;
        int at = 0;
        boolean feasible = true;
        for (int i = 0; i < n; i++) {
            Place p = node(route.get(i));
            travelTo[i] = p.isLunch() ? 0 : travel[at][p.location()];
            arrival[i] = time + travelTo[i];
            start[i] = Math.max(arrival[i], p.open());
            wait[i] = start[i] - arrival[i];
            feasible &= start[i] <= p.latestStart() && !p.closedOn(day);
            time = start[i] + p.serviceMinutes();
            if (!p.isLunch()) {
                at = p.location();
            }
        }
        int returnTravel = travel[at][0];
        int returnAt = time + returnTravel;
        feasible &= returnAt <= dayEnd;

        // MaxDelay_i = min(C_i − Start_i, Wait_{i+1} + MaxDelay_{i+1}), tính ngược từ lúc về nơi lưu trú
        int returnMaxDelay = dayEnd - returnAt;
        int nextWait = 0;
        int nextMaxDelay = returnMaxDelay;
        for (int i = n - 1; i >= 0; i--) {
            maxDelay[i] = Math.min(node(route.get(i)).latestStart() - start[i], nextWait + nextMaxDelay);
            nextWait = wait[i];
            nextMaxDelay = maxDelay[i];
        }
        return new Timeline(travelTo, arrival, start, wait, maxDelay, returnAt, returnTravel, returnMaxDelay, feasible);
    }

    /** Vị trí khách đang đứng ngay trước chỗ {@code pos} của tuyến (bữa trưa không đổi vị trí). */
    int locationBefore(List<Integer> route, int pos) {
        for (int i = pos - 1; i >= 0; i--) {
            if (route.get(i) != LUNCH) {
                return places.get(route.get(i)).location();
            }
        }
        return 0;
    }

    /** Vị trí thật kế tiếp từ chỗ {@code pos} trở đi; hết tuyến thì là nơi lưu trú. */
    int locationAfter(List<Integer> route, int pos) {
        for (int i = pos; i < route.size(); i++) {
            if (route.get(i) != LUNCH) {
                return places.get(route.get(i)).location();
            }
        }
        return 0;
    }
}
