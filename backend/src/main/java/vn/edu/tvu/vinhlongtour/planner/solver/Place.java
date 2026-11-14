package vn.edu.tvu.vinhlongtour.planner.solver;

import java.util.Set;

/**
 * Một nút của bài toán TOPTW, thời gian tính bằng phút kể từ 0 giờ: điểm tham quan thật, hoặc bữa trưa ảo
 * ({@code location = HERE}) diễn ra ngay tại chỗ khách đang đứng. Bất biến.
 *
 * @param score          điểm hấp dẫn S_i
 * @param serviceMinutes thời gian tham quan T_i
 * @param open           giờ sớm nhất bắt đầu được O_i
 * @param latestStart    giờ muộn nhất còn bắt đầu được C_i
 * @param cost           chi phí cho cả đoàn khi ghé điểm này
 * @param location       chỉ số vị trí trong bảng thời gian di chuyển (0 là nơi lưu trú)
 * @param closedOnDays   các ngày (đếm từ 0) điểm này đóng cửa
 */
public record Place(int score, int serviceMinutes, int open, int latestStart, long cost, int location, Set<Integer> closedOnDays) {

    /** Vị trí "tại chỗ": bữa trưa không làm khách di chuyển. */
    public static final int HERE = -1;

    public boolean isLunch() {
        return location == HERE;
    }

    public boolean closedOn(int day) {
        return closedOnDays.contains(day);
    }
}
