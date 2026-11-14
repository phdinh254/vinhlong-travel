package vn.edu.tvu.vinhlongtour.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Đồng hồ dùng chung theo giờ Việt Nam. Nghiệp vụ lấy "hôm nay", "bây giờ" từ bean này
 * thay vì gọi {@code LocalDate.now()} trực tiếp, để ngày luôn đúng múi giờ dù máy chủ đặt ở đâu.
 * Bật luôn các tác vụ chạy định kỳ ({@code @Scheduled}), ví dụ trả chỗ cho đơn quá hạn giữ chỗ.
 */
@Configuration
@EnableScheduling
public class TimeConfig {

    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(VIETNAM);
    }
}
