package vn.edu.tvu.vinhlongtour.planner.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.catalog.domain.InfoQuality;
import vn.edu.tvu.vinhlongtour.planner.domain.LodgingPreset;
import vn.edu.tvu.vinhlongtour.planner.domain.Pace;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourCard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Dữ liệu vào/ra của các API gợi ý lịch trình và lịch trình đã lưu.
 */
public final class TripDtos {

    private TripDtos() {
    }

    // ---------- Request ----------

    /**
     * Nơi lưu trú: chọn {@code lodging} trong danh sách gợi ý sẵn, hoặc gửi cả {@code lodgingLatitude}
     * và {@code lodgingLongitude} (tọa độ tự chọn trên bản đồ). {@code budget} để trống là không giới hạn.
     */
    public record SuggestRequest(
            @NotNull LocalDate startDate,
            @NotNull @Min(1) @Max(5) Integer days,
            @NotNull Pace pace,
            LodgingPreset lodging,
            Double lodgingLatitude,
            Double lodgingLongitude,
            @NotNull @Min(1) @Max(50) Integer adults,
            @Min(0) @Max(50) Integer children,
            @Min(0) Long budget,
            Set<Long> categoryIds,
            Set<Long> regionIds) {}

    public record SaveTripRequest(@NotBlank @Size(max = 150) String title, @NotNull @Valid SuggestRequest request) {}

    // ---------- Response ----------

    public record LodgingOption(LodgingPreset code, String name, double latitude, double longitude) {}

    public record LodgingInfo(LodgingPreset code, String name, double latitude, double longitude) {}

    /** Lịch trình gợi ý. {@code totalScore} là tổng điểm hấp dẫn thuật toán tối đa hóa. */
    public record TripPlan(LocalDate startDate, int days, Pace pace, LodgingInfo lodging, int adults, int children, Long budget,
                           int totalScore, long estimatedCost, int totalTravelMinutes, List<DayPlan> dayPlans,
                           List<SimilarTour> similarTours) {

        public TripPlan withSimilarTours(List<SimilarTour> tours) {
            return new TripPlan(startDate, days, pace, lodging, adults, children, budget, totalScore, estimatedCost,
                    totalTravelMinutes, dayPlans, tours);
        }
    }

    public record DayPlan(int dayNumber, LocalDate date, LocalTime leaveLodgingAt, LocalTime returnAt,
                          int returnTravelMinutes, int travelMinutes, long cost, List<Stop> stops) {}

    public enum StopType { VISIT, LUNCH }

    /**
     * Một chặng trong ngày. Bữa trưa ăn ngay tại chỗ vừa tham quan nên không có tọa độ, không tốn đường đi.
     * {@code hoursQuality}, {@code priceQuality}: độ tin cậy giờ mở cửa và giá vé, để giao diện ghi chú "ước tính".
     */
    public record Stop(StopType type, Long destinationId, String destinationCode, String name, Double latitude, Double longitude,
                       int travelMinutes, LocalTime arrival, LocalTime start, LocalTime leave, int waitMinutes,
                       int score, long cost, InfoQuality hoursQuality, InfoQuality priceQuality) {}

    /** Tour đang bán giống lịch trình; {@code similarity} = số điểm đến chung ÷ số điểm đến gộp lại (Jaccard). */
    public record SimilarTour(double similarity, TourCard tour) {}

    public record SavedTripSummary(Long id, String title, LocalDate startDate, int days, LocalDateTime createdAt) {}

    public record SavedTripDetail(Long id, String title, LocalDateTime createdAt, SuggestRequest request, TripPlan plan) {}
}
