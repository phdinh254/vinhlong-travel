package vn.edu.tvu.vinhlongtour.booking.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingStatus;
import vn.edu.tvu.vinhlongtour.booking.domain.Gender;
import vn.edu.tvu.vinhlongtour.booking.domain.Passenger;
import vn.edu.tvu.vinhlongtour.booking.domain.PassengerStatus;
import vn.edu.tvu.vinhlongtour.catalog.domain.SurchargeMethod;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;

/**
 * Dữ liệu vào/ra của các API đặt tour.
 */
public final class BookingDtos {

    private static final String PHONE = "^[0-9+]{9,15}$";
    private static final String OPTIONAL_PHONE = "^$|^[0-9+]{9,15}$";

    private BookingDtos() {
    }

    // ---------- Request ----------

    public record PassengerInput(
            @NotBlank @Size(max = 150) String fullName,
            @NotNull @PastOrPresent LocalDate birthDate,
            Gender gender,
            @Pattern(regexp = OPTIONAL_PHONE, message = "số điện thoại không hợp lệ") String phone,
            @Size(max = 30) String idNumber,
            Long pickupPointId) {}

    public record SurchargeSelection(@NotNull Long surchargeId, @Min(1) Integer quantity) {}

    /** Báo giá trước khi đặt: không giữ chỗ, không cần thông tin liên hệ. */
    public record QuoteRequest(
            @NotNull Long departureId,
            @NotEmpty @Size(max = 50) @Valid List<PassengerInput> passengers,
            @Valid List<SurchargeSelection> surcharges) {}

    public record BookingRequest(
            @NotNull Long departureId,
            @NotBlank @Size(max = 150) String contactName,
            @NotBlank @Pattern(regexp = PHONE, message = "số điện thoại không hợp lệ") String contactPhone,
            @Email @Size(max = 150) String contactEmail,
            @Size(max = 1000) String note,
            @NotEmpty @Size(max = 50) @Valid List<PassengerInput> passengers,
            @Valid List<SurchargeSelection> surcharges) {

        public QuoteRequest toQuote() {
            return new QuoteRequest(departureId, passengers, surcharges);
        }
    }

    /** Admin chỉ đổi trực tiếp sang COMPLETED (sau chuyến đi) hoặc CANCELLED (đơn chưa thu tiền). */
    public record StatusChangeRequest(@NotNull BookingStatus status, @Size(max = 500) String reason) {}

    // ---------- Response ----------

    public record QuoteResponse(List<PassengerQuote> passengers, List<SurchargeLine> surcharges,
                                long ticketTotal, long pickupTotal, long surchargeTotal, long totalAmount,
                                int seats, long minimumFirstPayment) {}

    public record PassengerQuote(String fullName, int age, Long passengerTypeId, String passengerTypeName,
                                 long price, String pickupPointName, long pickupFee) {}

    public record SurchargeLine(Long surchargeId, String name, SurchargeMethod method, int quantity, long unitPrice, long amount) {}

    public record BookingSummary(Long id, String code, BookingStatus status, String tourName, String departureCode,
                                 LocalDateTime departureAt, String contactName, String contactPhone, int passengerCount,
                                 long totalAmount, long paidAmount, LocalDateTime holdUntil, LocalDateTime createdAt) {

        public static BookingSummary from(Booking b) {
            Departure d = b.getDeparture();
            return new BookingSummary(b.getId(), b.getCode(), b.getStatus(), d.getTour().getName(), d.getCode(), d.getDepartureAt(),
                    b.getContactName(), b.getContactPhone(),
                    (int) b.getPassengers().stream().filter(p -> p.getStatus() == PassengerStatus.ACTIVE).count(),
                    b.getTotalAmount(), b.getPaidAmount(), b.getHoldUntil(), b.getCreatedAt());
        }
    }

    public record BookingDetail(Long id, String code, BookingStatus status,
                                TourRef tour, DepartureRef departure, ContactInfo contact, String note,
                                List<PassengerResponse> passengers, List<SurchargeLine> surcharges,
                                long ticketTotal, long pickupTotal, long surchargeTotal, long totalAmount,
                                long paidAmount, long remainingAmount, long minimumFirstPayment, int seats,
                                LocalDateTime holdUntil, LocalDateTime createdAt,
                                CancellationPolicyResponse cancellationPolicy, List<HistoryEntry> history) {

        /** @param minimumFirstPayment số tiền tối thiểu cần trả tiếp: tiền cọc nếu chưa trả gì, còn lại nếu đã trả */
        public static BookingDetail from(Booking b, long minimumFirstPayment) {
            Departure d = b.getDeparture();
            LocalDate departureDate = d.getDepartureAt().toLocalDate();
            return new BookingDetail(b.getId(), b.getCode(), b.getStatus(),
                    new TourRef(d.getTour().getId(), d.getTour().getCode(), d.getTour().getName()),
                    new DepartureRef(d.getId(), d.getCode(), d.getDepartureAt(), d.getReturnAt()),
                    new ContactInfo(b.getContactName(), b.getContactPhone(), b.getContactEmail()), b.getNote(),
                    b.getPassengers().stream().map(p -> PassengerResponse.from(p, departureDate)).toList(),
                    b.getSurcharges().stream().map(s -> new SurchargeLine(s.getSurcharge().getId(), s.getName(), s.getMethod(),
                            s.getQuantity(), s.getUnitPrice(), s.getAmount())).toList(),
                    b.getTicketTotal(), b.getPickupTotal(), b.getSurchargeTotal(), b.getTotalAmount(),
                    b.getPaidAmount(), b.remainingAmount(), minimumFirstPayment, b.getSeats(),
                    b.getHoldUntil(), b.getCreatedAt(),
                    b.getCancellationPolicy() == null ? null : CancellationPolicyResponse.from(b.getCancellationPolicy()),
                    b.getHistory().stream().map(h -> new HistoryEntry(h.getFromStatus(), h.getToStatus(), h.getChangedAt(),
                            h.getChangedBy() == null ? null : h.getChangedBy().getFullName(), h.getReason())).toList());
        }
    }

    public record TourRef(Long id, String code, String name) {}

    public record DepartureRef(Long id, String code, LocalDateTime departureAt, LocalDateTime returnAt) {}

    public record ContactInfo(String name, String phone, String email) {}

    public record PassengerResponse(Long id, String fullName, LocalDate birthDate, int ageAtDeparture, Gender gender,
                                    String phone, String idNumber, String passengerTypeName, long price,
                                    String pickupPointName, LocalTime pickupTime, long pickupFee, PassengerStatus status) {

        static PassengerResponse from(Passenger p, LocalDate departureDate) {
            return new PassengerResponse(p.getId(), p.getFullName(), p.getBirthDate(),
                    Period.between(p.getBirthDate(), departureDate).getYears(), p.getGender(), p.getPhone(), p.getIdNumber(),
                    p.getPassengerType().getName(), p.getPrice(),
                    p.getPickupPoint() == null ? null : p.getPickupPoint().getName(), p.getPickupTime(), p.getPickupFee(), p.getStatus());
        }
    }

    /** {@code changedBy} null nghĩa là hệ thống tự động (ví dụ hết hạn giữ chỗ). */
    public record HistoryEntry(BookingStatus fromStatus, BookingStatus toStatus, LocalDateTime changedAt, String changedBy, String reason) {}
}
