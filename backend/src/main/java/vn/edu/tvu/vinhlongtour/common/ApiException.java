package vn.edu.tvu.vinhlongtour.common;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ mà service ném ra để trả về cho client, ví dụ "đợt khởi hành hết chỗ".
 * Mang theo mã HTTP và một mã lỗi ổn định ({@code code}) để frontend dựa vào đó hiển thị,
 * không phải đọc câu thông báo.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
