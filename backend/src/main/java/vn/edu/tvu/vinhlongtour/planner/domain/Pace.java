package vn.edu.tvu.vinhlongtour.planner.domain;

import java.time.LocalTime;

/**
 * Nhịp đi khách chọn, quyết định quỹ giờ mỗi ngày (giờ rời nơi lưu trú, giờ phải về).
 */
public enum Pace {

    /** Thong thả. */
    RELAXED(LocalTime.of(8, 0), LocalTime.of(16, 0)),
    /** Vừa phải. */
    MODERATE(LocalTime.of(7, 30), LocalTime.of(17, 30)),
    /** Đi dày. */
    PACKED(LocalTime.of(7, 0), LocalTime.of(19, 0));

    private final LocalTime dayStart;
    private final LocalTime dayEnd;

    Pace(LocalTime dayStart, LocalTime dayEnd) {
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
    }

    public LocalTime dayStart() {
        return dayStart;
    }

    public LocalTime dayEnd() {
        return dayEnd;
    }
}
