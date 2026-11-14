package vn.edu.tvu.vinhlongtour.cancellation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequest;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationRequestStatus;
import vn.edu.tvu.vinhlongtour.booking.domain.CancellationScope;
import vn.edu.tvu.vinhlongtour.booking.domain.Passenger;
import vn.edu.tvu.vinhlongtour.booking.domain.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Dữ liệu vào/ra của các API hủy tour, hoàn tiền, chuyển đợt.
 */
public final class CancellationDtos {

    private CancellationDtos() {
    }

    /** Khách vãng lai gửi kèm {@code phone}; hủy một phần thì chọn {@code passengerIds}. */
    public record CancellationInput(@NotNull CancellationScope scope, Set<Long> passengerIds,
                                    @Size(max = 1000) String reason, String phone) {}

    public record DecisionRequest(@Size(max = 500) String note) {}

    public record RefundRequest(
            @NotNull @Min(1) Long amount,
            @NotNull PaymentMethod method,
            @Size(max = 50) String transactionNo,
            @Size(max = 500) @Pattern(regexp = "^$|^(https?://|/uploads/).+", message = "đường dẫn ảnh không hợp lệ") String proofUrl,
            @Size(max = 500) String note,
            Long cancellationRequestId) {}

    public record TransferRequest(@NotNull Long departureId, @Size(max = 500) String reason) {}

    /** Một dòng trong danh sách yêu cầu hủy của admin. */
    public record CancellationRequestSummary(Long id, Long bookingId, String bookingCode, String tourName, LocalDateTime departureAt,
                                             CancellationScope scope, CancellationRequestStatus status, List<String> passengerNames,
                                             int penaltyPercent, long penaltyAmount, long estimatedRefund, long paidAmount,
                                             String reason, LocalDateTime requestedAt) {

        public static CancellationRequestSummary from(CancellationRequest r) {
            var b = r.getBooking();
            return new CancellationRequestSummary(r.getId(), b.getId(), b.getCode(), b.getDeparture().getTour().getName(),
                    b.getDeparture().getDepartureAt(), r.getScope(), r.getStatus(),
                    r.getPassengers().stream().map(Passenger::getFullName).toList(),
                    r.getPenaltyPercent(), r.getPenaltyAmount(), r.getEstimatedRefund(), b.getPaidAmount(),
                    r.getReason(), r.getRequestedAt());
        }
    }
}
