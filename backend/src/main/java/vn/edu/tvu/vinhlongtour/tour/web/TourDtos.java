package vn.edu.tvu.vinhlongtour.tour.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.domain.NamedItem;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.NamedItemResponse;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.domain.Itinerary;
import vn.edu.tvu.vinhlongtour.tour.domain.ItineraryActivity;
import vn.edu.tvu.vinhlongtour.tour.domain.ItineraryDay;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;
import vn.edu.tvu.vinhlongtour.tour.domain.TourAmenity;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository.TourPriceSummary;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Dữ liệu vào/ra của các API tour, lịch trình và đợt khởi hành.
 */
public final class TourDtos {

    private TourDtos() {
    }

    // ---------- Request ----------

    public record TourRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String summary,
            @Size(max = 5000) String description,
            @NotNull @Min(1) @Max(30) Integer durationDays,
            @NotNull @Min(0) Integer durationNights,
            @NotEmpty Set<Long> regionIds,
            @NotEmpty Set<Long> categoryIds,
            @Valid List<AmenityLine> amenities,
            Set<Long> surchargeIds,
            Boolean featured,
            TourStatus status) {}

    public record AmenityLine(@NotNull Long amenityId, @NotNull Boolean included) {}

    public record ImagesRequest(@NotNull @Size(max = 20) @Valid List<ImageLine> images) {}

    /** Ảnh là đường dẫn http(s), ảnh đã tải lên máy chủ ({@code /uploads/...}) hoặc ảnh có sẵn trong frontend ({@code /images/...}). */
    public record ImageLine(
            @NotBlank @Size(max = 500) @Pattern(regexp = "^(https?://|/uploads/|/images/).+", message = "đường dẫn ảnh không hợp lệ") String url,
            @Size(max = 200) String caption,
            Boolean cover) {}

    public record ItineraryRequest(@NotNull @Size(max = 30) @Valid List<DayLine> days) {}

    public record DayLine(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String description,
                          @Valid List<ActivityLine> activities) {}

    public record ActivityLine(LocalTime startTime, @NotBlank @Size(max = 200) String title,
                               @Size(max = 2000) String description, Long destinationId) {}

    public record DepartureRequest(
            @NotNull LocalDateTime departureAt,
            @NotNull LocalDateTime returnAt,
            @NotNull LocalDateTime bookingDeadline,
            @NotNull @Min(1) @Max(1000) Integer capacity,
            DepartureStatus status,
            @NotEmpty @Valid List<PriceLine> prices,
            @Valid List<PickupLine> pickups) {}

    public record PriceLine(@NotNull Long passengerTypeId, @NotNull @Min(0) Long price) {}

    public record PickupLine(@NotNull Long pickupPointId, @NotNull LocalTime pickupTime, @NotNull @Min(0) Long fee) {}

    // ---------- Response: tour ----------

    /** Thẻ tour ở trang danh sách. {@code fromPrice}, {@code nextDepartureAt} null khi chưa có đợt nào đặt được. */
    public record TourCard(Long id, String code, String name, String summary, int durationDays, int durationNights,
                           String coverImageUrl, List<NamedItemResponse> regions, List<NamedItemResponse> categories,
                           boolean featured, TourStatus status, Long fromPrice, LocalDateTime nextDepartureAt) {

        public static TourCard from(Tour t, TourPriceSummary summary) {
            return new TourCard(t.getId(), t.getCode(), t.getName(), t.getSummary(), t.getDurationDays(), t.getDurationNights(),
                    t.coverImageUrl(), names(t.getRegions()), names(t.getCategories()), t.isFeatured(), t.getStatus(),
                    summary == null ? null : summary.getFromPrice(), summary == null ? null : summary.getNextDepartureAt());
        }
    }

    public record TourDetail(Long id, String code, String name, String summary, String description,
                             int durationDays, int durationNights, TourStatus status, boolean featured,
                             List<NamedItemResponse> regions, List<NamedItemResponse> categories, List<ImageLine> images,
                             List<NamedItemResponse> includedAmenities, List<NamedItemResponse> excludedAmenities,
                             List<SurchargeResponse> surcharges, ItineraryResponse itinerary) {

        public static TourDetail from(Tour t) {
            return new TourDetail(t.getId(), t.getCode(), t.getName(), t.getSummary(), t.getDescription(),
                    t.getDurationDays(), t.getDurationNights(), t.getStatus(), t.isFeatured(),
                    names(t.getRegions()), names(t.getCategories()),
                    t.getImages().stream().map(i -> new ImageLine(i.url(), i.caption(), i.cover())).toList(),
                    amenities(t.getAmenities(), true), amenities(t.getAmenities(), false),
                    t.getSurcharges().stream().map(SurchargeResponse::from).sorted(Comparator.comparing(SurchargeResponse::name)).toList(),
                    ItineraryResponse.from(t.getItinerary()));
        }

        private static List<NamedItemResponse> amenities(List<TourAmenity> all, boolean included) {
            return all.stream().filter(a -> a.isIncluded() == included)
                    .map(a -> NamedItemResponse.from(a.getAmenity()))
                    .sorted(Comparator.comparing(NamedItemResponse::name)).toList();
        }
    }

    // ---------- Response: lịch trình ----------

    public record ItineraryResponse(List<DayResponse> days) {
        public static ItineraryResponse from(Itinerary itinerary) {
            return new ItineraryResponse(itinerary.getDays().stream().map(DayResponse::from).toList());
        }
    }

    public record DayResponse(int dayNumber, String title, String description, List<ActivityResponse> activities) {
        static DayResponse from(ItineraryDay d) {
            return new DayResponse(d.getDayNumber(), d.getTitle(), d.getDescription(),
                    d.getActivities().stream().map(ActivityResponse::from).toList());
        }
    }

    public record ActivityResponse(LocalTime startTime, String title, String description, DestinationRef destination) {
        static ActivityResponse from(ItineraryActivity a) {
            Destination d = a.getDestination();
            return new ActivityResponse(a.getStartTime(), a.getTitle(), a.getDescription(),
                    d == null ? null : new DestinationRef(d.getId(), d.getCode(), d.getName()));
        }
    }

    public record DestinationRef(Long id, String code, String name) {}

    // ---------- Response: đợt khởi hành ----------

    public record DepartureResponse(Long id, String code, Long tourId, String tourName,
                                    LocalDateTime departureAt, LocalDateTime returnAt, LocalDateTime bookingDeadline,
                                    int capacity, int bookedSeats, int seatsLeft, DepartureStatus status, boolean bookable,
                                    List<PriceResponse> prices, List<PickupResponse> pickups) {

        public static DepartureResponse from(Departure d, LocalDateTime now) {
            return new DepartureResponse(d.getId(), d.getCode(), d.getTour().getId(), d.getTour().getName(),
                    d.getDepartureAt(), d.getReturnAt(), d.getBookingDeadline(),
                    d.getCapacity(), d.getBookedSeats(), d.seatsLeft(), d.getStatus(), d.isBookable(now),
                    d.getPrices().stream()
                            .map(p -> new PriceResponse(p.getPassengerType().getId(), p.getPassengerType().getName(),
                                    p.getPassengerType().getAgeRange().minAge(), p.getPassengerType().getAgeRange().maxAge(), p.getPrice()))
                            .sorted(Comparator.comparingInt(PriceResponse::minAge).reversed()).toList(),
                    d.getPickups().stream()
                            .map(p -> new PickupResponse(p.getPickupPoint().getId(), p.getPickupPoint().getName(),
                                    p.getPickupPoint().getAddress(), p.getPickupTime(), p.getFee()))
                            .sorted(Comparator.comparing(PickupResponse::pickupTime)).toList());
        }
    }

    public record PriceResponse(Long passengerTypeId, String passengerTypeName, int minAge, int maxAge, long price) {}

    public record PickupResponse(Long pickupPointId, String name, String address, LocalTime pickupTime, long fee) {}

    private static List<NamedItemResponse> names(Collection<? extends NamedItem> items) {
        return items.stream().map(NamedItemResponse::from).sorted(Comparator.comparing(NamedItemResponse::name)).toList();
    }
}
