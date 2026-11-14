package vn.edu.tvu.vinhlongtour.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Đồng hồ dùng chung theo giờ Việt Nam. Nghiệp vụ lấy "hôm nay", "bây giờ" từ bean này
 * thay vì gọi {@code LocalDate.now()} trực tiếp, để ngày luôn đúng múi giờ dù máy chủ đặt ở đâu.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(VIETNAM);
    }
}
