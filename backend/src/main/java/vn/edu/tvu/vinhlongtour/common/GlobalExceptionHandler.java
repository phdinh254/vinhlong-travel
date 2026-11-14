package vn.edu.tvu.vinhlongtour.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt mọi lỗi phát sinh trong controller và đổi về cùng một dạng {@link ApiError},
 * để frontend chỉ phải xử lý một kiểu phản hồi lỗi.
 * Kế thừa {@link ResponseEntityExceptionHandler} để nhận luôn các lỗi chuẩn của Spring MVC
 * (sai phương thức HTTP, JSON hỏng, đường dẫn không tồn tại...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    // CSDL chặn ghi vì trùng giá trị duy nhất (hai người cùng đăng ký một email cùng lúc)
    // hoặc vì xóa bản ghi đang được bản ghi khác dùng: báo xung đột thay vì lỗi 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("DATA_CONFLICT",
                "Dữ liệu bị trùng, hoặc đang được dữ liệu khác sử dụng nên không thể xóa"));
    }

    // Hai người cùng sửa một bản ghi có cột version (ví dụ đợt khởi hành): người lưu sau bị từ chối thay vì ghi đè
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("CONCURRENT_UPDATE",
                "Dữ liệu vừa được người khác thay đổi, vui lòng tải lại rồi thử lại"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong đợi", ex);
        // Không trả chi tiết lỗi ra ngoài để tránh lộ thông tin bên trong hệ thống
        return ResponseEntity.internalServerError().body(new ApiError("INTERNAL_ERROR", "Lỗi hệ thống, vui lòng thử lại sau"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(e.getField(), e.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ", fields));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return ResponseEntity.status(status).headers(headers).body(new ApiError(status.name(), status.getReasonPhrase()));
    }
}
