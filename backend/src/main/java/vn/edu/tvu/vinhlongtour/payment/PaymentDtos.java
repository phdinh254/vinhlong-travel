package vn.edu.tvu.vinhlongtour.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.booking.domain.PaymentMethod;

import java.time.LocalDateTime;

/**
 * Dữ liệu vào/ra của các API thanh toán.
 */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    /** Khách muốn trả bao nhiêu: tiền cọc tối thiểu (chỉ khi chưa trả gì) hoặc toàn bộ số còn lại. */
    public enum PaymentType {
        DEPOSIT,
        FULL
    }

    /** Khách vãng lai gửi kèm {@code phone} để chứng minh là chủ đơn; khách đã đăng nhập thì không cần. */
    public record VnPayLinkRequest(@NotBlank String bookingCode, String phone, @NotNull PaymentType type) {}

    public record VnPayLinkResponse(String paymentUrl, long amount, String reference, LocalDateTime expiresAt) {}

    /** Kết quả hiển thị ở trang khách quay về sau khi thanh toán. */
    public record VnPayReturnResponse(boolean validSignature, boolean success, String bookingCode, Long amount,
                                      String transactionNo, String responseCode, String message) {}

    public record ManualPaymentRequest(
            @NotNull @Min(1) Long amount,
            @NotNull PaymentMethod method,
            @Size(max = 50) String transactionNo,
            @Size(max = 500) @Pattern(regexp = "^$|^(https?://|/uploads/).+", message = "đường dẫn ảnh không hợp lệ") String proofUrl,
            @Size(max = 500) String note) {}
}
