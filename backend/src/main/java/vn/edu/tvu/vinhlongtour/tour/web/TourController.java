package vn.edu.tvu.vinhlongtour.tour.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.tour.service.DepartureService;
import vn.edu.tvu.vinhlongtour.tour.service.TourService;
import vn.edu.tvu.vinhlongtour.tour.service.TourService.TourSearch;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourCard;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourDetail;

import java.util.List;

/**
 * API công khai cho khách: tìm/lọc tour đang bán, xem chi tiết, các đợt khởi hành và lịch trình của đợt.
 */
@RestController
@RequestMapping("/api/v1")
public class TourController {

    private final TourService tourService;
    private final DepartureService departureService;

    public TourController(TourService tourService, DepartureService departureService) {
        this.tourService = tourService;
        this.departureService = departureService;
    }

    /**
     * Tìm tour. {@code minDays}/{@code maxDays} lọc theo số ngày (ví dụ tour 3 ngày trở lên: {@code minDays=3});
     * {@code destinationId}: tour có ghé điểm đến này.
     */
    @GetMapping("/tours")
    public PageResponse<TourCard> search(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Long regionId,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Long amenityId,
                                         @RequestParam(required = false) Long destinationId,
                                         @RequestParam(required = false) Integer minDays,
                                         @RequestParam(required = false) Integer maxDays,
                                         @RequestParam(required = false) Boolean featured,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "12") int size) {
        return tourService.search(new TourSearch(q, regionId, categoryId, amenityId, destinationId, minDays, maxDays, featured, null, true),
                page, size);
    }

    @GetMapping("/tours/{id}")
    public TourDetail get(@PathVariable Long id) {
        return tourService.get(id, false);
    }

    @GetMapping("/tours/{id}/departures")
    public List<DepartureResponse> departures(@PathVariable Long id) {
        return departureService.listForCustomer(id);
    }

    @GetMapping("/departures/{id}")
    public DepartureResponse departure(@PathVariable Long id) {
        return departureService.get(id, false);
    }

    @GetMapping("/departures/{id}/itinerary")
    public ItineraryResponse departureItinerary(@PathVariable Long id) {
        return departureService.itinerary(id, false);
    }
}
