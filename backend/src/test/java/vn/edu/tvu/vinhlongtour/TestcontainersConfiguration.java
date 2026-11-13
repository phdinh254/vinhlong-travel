package vn.edu.tvu.vinhlongtour;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Cấu hình dùng chung cho các test tích hợp: chạy một PostgreSQL thật trong Docker
 * (cùng phiên bản với docker-compose.yml) và tự nối Spring vào CSDL đó.
 * Test nào cần CSDL thì thêm {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
