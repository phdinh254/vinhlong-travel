package vn.edu.tvu.vinhlongtour.catalog.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.catalog.domain.CancellationPolicy;
import vn.edu.tvu.vinhlongtour.catalog.domain.CoordinateQuality;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.domain.InfoQuality;
import vn.edu.tvu.vinhlongtour.catalog.domain.NamedItem;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.PenaltyTier;
import vn.edu.tvu.vinhlongtour.catalog.domain.PickupPoint;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.catalog.domain.SurchargeMethod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Dữ liệu vào/ra của các API danh mục. Request được kiểm tra bằng {@code @Valid};
 * response chỉ chứa dữ liệu cần hiển thị, không lộ entity ra ngoài.
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    // ---------- Khu vực, loại hình, dịch vụ ----------

    public record NamedItemRequest(@NotBlank @Size(max = 100) String name) {}

    public record NamedItemResponse(Long id, String name) {
        public static NamedItemResponse from(NamedItem item) {
            return new NamedItemResponse(item.getId(), item.getName());
        }
    }

    // ---------- Loại khách ----------

    public record PassengerTypeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull @Min(0) Integer minAge,
            @NotNull @Min(0) Integer maxAge,
            @NotNull @Min(0) @Max(2) Integer seats) {}

    public record PassengerTypeResponse(Long id, String name, int minAge, int maxAge, int seats) {
        public static PassengerTypeResponse from(PassengerType p) {
            return new PassengerTypeResponse(p.getId(), p.getName(), p.getAgeRange().minAge(), p.getAgeRange().maxAge(), p.getSeats());
        }
    }

    // ---------- Điểm đón ----------

    public record PickupPointRequest(@NotBlank @Size(max = 150) String name, @Size(max = 300) String address, Boolean active) {}

    public record PickupPointResponse(Long id, String name, String address, boolean active) {
        public static PickupPointResponse from(PickupPoint p) {
            return new PickupPointResponse(p.getId(), p.getName(), p.getAddress(), p.isActive());
        }
    }

    // ---------- Phụ thu ----------

    public record SurchargeRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull SurchargeMethod method,
            @NotNull @Min(0) Long unitPrice,
            @Size(max = 500) String description,
            Boolean active) {}

    public record SurchargeResponse(Long id, String name, SurchargeMethod method, long unitPrice, String description, boolean active) {
        public static SurchargeResponse from(Surcharge s) {
            return new SurchargeResponse(s.getId(), s.getName(), s.getMethod(), s.getUnitPrice(), s.getDescription(), s.isActive());
        }
    }

    // ---------- Chính sách hủy ----------

    public record PenaltyTierDto(@NotNull Integer daysBeforeFrom, @NotNull Integer daysBeforeTo, @NotNull Integer penaltyPercent) {
        public PenaltyTier toTier() {
            return new PenaltyTier(daysBeforeFrom, daysBeforeTo, penaltyPercent);
        }
    }

    public record CancellationPolicyRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull LocalDate effectiveFrom,
            @Size(max = 1000) String description,
            @NotEmpty @Valid List<PenaltyTierDto> tiers) {}

    public record CancellationPolicyResponse(Long id, String name, LocalDate effectiveFrom, String description,
                                             List<PenaltyTierDto> tiers) {
        public static CancellationPolicyResponse from(CancellationPolicy p) {
            return new CancellationPolicyResponse(p.getId(), p.getName(), p.getEffectiveFrom(), p.getDescription(),
                    p.getTiers().stream().map(t -> new PenaltyTierDto(t.daysBeforeFrom(), t.daysBeforeTo(), t.penaltyPercent())).toList());
        }
    }

    // ---------- Điểm đến ----------

    public record DestinationRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 150) String name,
            @NotNull Long regionId,
            @Size(max = 300) String address,
            @NotNull Double latitude,
            @NotNull Double longitude,
            @NotNull CoordinateQuality coordinateQuality,
            @NotEmpty Set<Long> categoryIds,
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            Set<DayOfWeek> closedDays,
            @NotNull InfoQuality hoursQuality,
            @Min(0) Long ticketPriceAdult,
            @Min(0) Long ticketPriceChild,
            @NotNull InfoQuality priceQuality,
            @NotNull @Min(1) @Max(600) Integer visitMinutes,
            @NotNull @Min(1) @Max(5) Integer popularity,
            @Size(max = 2000) String description,
            @Size(max = 500) String sources,
            Boolean active) {}

    public record DestinationResponse(
            Long id, String code, String name, NamedItemResponse region, String address,
            double latitude, double longitude, CoordinateQuality coordinateQuality,
            List<NamedItemResponse> categories,
            LocalTime openTime, LocalTime closeTime, Set<DayOfWeek> closedDays, InfoQuality hoursQuality,
            Long ticketPriceAdult, Long ticketPriceChild, InfoQuality priceQuality,
            int visitMinutes, int popularity, String description, String sources, boolean active) {

        public static DestinationResponse from(Destination d) {
            return new DestinationResponse(d.getId(), d.getCode(), d.getName(), NamedItemResponse.from(d.getRegion()), d.getAddress(),
                    d.getLocation().latitude(), d.getLocation().longitude(), d.getCoordinateQuality(),
                    d.getCategories().stream().map(NamedItemResponse::from).sorted(Comparator.comparing(NamedItemResponse::name)).toList(),
                    d.getOpeningHours().openTime(), d.getOpeningHours().closeTime(), d.getClosedDays(), d.getHoursQuality(),
                    d.getTicketPriceAdult(), d.getTicketPriceChild(), d.getPriceQuality(),
                    d.getVisitMinutes(), d.getPopularity(), d.getDescription(), d.getSources(), d.isActive());
        }
    }
}
