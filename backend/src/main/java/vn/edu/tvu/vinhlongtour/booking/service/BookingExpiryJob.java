package vn.edu.tvu.vinhlongtour.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Mỗi phút trả chỗ cho các đơn đã quá hạn giữ chỗ mà chưa thanh toán. Mỗi đơn xử lý trong giao dịch riêng,
 * nên một đơn lỗi không làm hỏng các đơn khác.
 */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;
    private final Clock clock;

    public BookingExpiryJob(BookingService bookingService, Clock clock) {
        this.bookingService = bookingService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        expireOverdueHolds(LocalDateTime.now(clock));
    }

    /** @return số đơn đã chuyển sang hết hạn */
    public int expireOverdueHolds(LocalDateTime now) {
        int expired = 0;
        for (Long id : bookingService.overdueHoldIds(now)) {
            try {
                if (bookingService.expireIfOverdue(id, now)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Không trả được chỗ cho đơn {}", id, e);
            }
        }
        if (expired > 0) {
            log.info("Đã trả chỗ cho {} đơn quá hạn giữ chỗ", expired);
        }
        return expired;
    }
}
