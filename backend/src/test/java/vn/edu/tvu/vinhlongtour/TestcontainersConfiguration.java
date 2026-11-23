package vn.edu.tvu.vinhlongtour;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Cấu hình dùng chung cho các test tích hợp: chạy một MySQL thật trong Docker
 * (cùng phiên bản với docker-compose.yml) và tự nối Spring vào CSDL đó.
 * Test nào cần CSDL thì thêm {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        // Cùng bảng mã và cách so sánh chữ với docker-compose.yml
        return new MySQLContainer("mysql:8.4")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_as_ci");
    }
}
