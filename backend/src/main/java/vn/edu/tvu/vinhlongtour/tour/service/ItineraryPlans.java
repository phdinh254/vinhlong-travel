package vn.edu.tvu.vinhlongtour.tour.service;

import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.repository.DestinationRepository;
import vn.edu.tvu.vinhlongtour.common.Lookups;
import vn.edu.tvu.vinhlongtour.tour.domain.Itinerary;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ActivityLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Đổi lịch trình client gửi lên thành dữ liệu domain ({@link Itinerary.DayPlan}), tra các điểm đến
 * bằng một truy vấn. Dùng chung cho lịch trình mẫu của tour và lịch trình riêng của đợt.
 */
@Component
class ItineraryPlans {

    private final DestinationRepository destinations;

    ItineraryPlans(DestinationRepository destinations) {
        this.destinations = destinations;
    }

    List<Itinerary.DayPlan> from(ItineraryRequest r) {
        List<Long> ids = r.days().stream()
                .flatMap(d -> activities(d.activities()).stream())
                .map(ActivityLine::destinationId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Destination> byId = Lookups.all(destinations, ids, "DESTINATION_NOT_FOUND", "Có điểm đến không tồn tại").stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity()));
        return r.days().stream().map(d -> new Itinerary.DayPlan(d.title(), d.description(),
                activities(d.activities()).stream().map(a -> new Itinerary.ActivityPlan(a.startTime(), a.title(), a.description(),
                        a.destinationId() == null ? null : byId.get(a.destinationId()))).toList())).toList();
    }

    private static List<ActivityLine> activities(List<ActivityLine> activities) {
        return activities == null ? List.of() : activities;
    }
}
