package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Embeddable;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalTime;

/**
 * Khung giờ mở cửa trong ngày [openTime, closeTime). Bất biến.
 * Gợi ý lịch trình dùng khung giờ này để không xếp khách tới lúc điểm đến đã đóng cửa.
 */
@Embeddable
public record TimeWindow(LocalTime openTime, LocalTime closeTime) {

    public TimeWindow {
        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            throw ApiException.badRequest("INVALID_OPENING_HOURS", "Giờ mở cửa phải trước giờ đóng cửa");
        }
    }
}
