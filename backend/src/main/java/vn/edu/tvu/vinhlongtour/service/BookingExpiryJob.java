package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.repository.DonDatTourRepository;

import java.time.LocalDateTime;

/** Q31: mỗi phút quét đơn quá hạn giữ chỗ để trả lại chỗ */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryJob {
    private final DonDatTourRepository donRepo;
    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        for (Integer id : donRepo.findIdsHetHanGiuCho(LocalDateTime.now())) {
            try {
                bookingService.expireIfNeeded(id);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý hết hạn đơn {}", id, e);
            }
        }
    }
}
