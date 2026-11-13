package vn.edu.tvu.vinhlongtour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của backend VinhLong Tour.
 * Spring Boot quét mọi class trong gói {@code vn.edu.tvu.vinhlongtour} và các gói con.
 */
@SpringBootApplication
public class VinhLongTourApplication {

    public static void main(String[] args) {
        SpringApplication.run(VinhLongTourApplication.class, args);
    }
}
