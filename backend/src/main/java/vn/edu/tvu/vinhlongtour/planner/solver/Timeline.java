package vn.edu.tvu.vinhlongtour.planner.solver;

/**
 * Lịch chi tiết của một ngày (một tuyến), mỗi mảng có một phần tử cho mỗi nút theo thứ tự đi.
 * Tên biến theo bài báo: Arrival (giờ đến), Start (giờ bắt đầu), Wait — trường {@code waiting} (phải chờ mở cửa),
 * MaxDelay (nút này còn lùi giờ bắt đầu được bao lâu mà không làm hỏng các nút sau).
 *
 * @param travel         phút đi đường từ nút trước (hoặc nơi lưu trú) tới nút này
 * @param returnAt       giờ về tới nơi lưu trú
 * @param returnTravel   phút đi đường từ nút cuối về nơi lưu trú
 * @param returnMaxDelay MaxDelay của nơi lưu trú lúc về = giờ hết ngày − giờ về
 * @param feasible       mọi nút bắt đầu trong khung giờ, không ghé điểm đóng cửa, về trước giờ hết ngày
 */
public record Timeline(int[] travel, int[] arrival, int[] start, int[] waiting, int[] maxDelay,
                       int returnAt, int returnTravel, int returnMaxDelay, boolean feasible) {
}
