package vn.edu.tvu.vinhlongtour.tour.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.service.DepartureService;
import vn.edu.tvu.vinhlongtour.tour.service.TourService;
import vn.edu.tvu.vinhlongtour.tour.service.TourService.TourSearch;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ImagesRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourCard;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourDetail;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourRequest;

import java.util.List;

/**
 * API quản trị tour và đợt khởi hành (chỉ ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminTourController {

    private final TourService tourService;
    private final DepartureService departureService;

    public AdminTourController(TourService tourService, DepartureService departureService) {
        this.tourService = tourService;
        this.departureService = departureService;
    }

    // ---------- Tour ----------

    @GetMapping("/tours")
    public PageResponse<TourCard> search(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) TourStatus status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return tourService.search(new TourSearch(q, null, null, null, null, null, null, null, status, false), page, size);
    }

    @GetMapping("/tours/{id}")
    public TourDetail get(@PathVariable Long id) {
        return tourService.get(id, true);
    }

    @PostMapping("/tours")
    @ResponseStatus(HttpStatus.CREATED)
    public TourDetail create(@Valid @RequestBody TourRequest r) {
        return tourService.create(r);
    }

    @PutMapping("/tours/{id}")
    public TourDetail update(@PathVariable Long id, @Valid @RequestBody TourRequest r) {
        return tourService.update(id, r);
    }

    @PutMapping("/tours/{id}/images")
    public TourDetail replaceImages(@PathVariable Long id, @Valid @RequestBody ImagesRequest r) {
        return tourService.replaceImages(id, r);
    }

    @PutMapping("/tours/{id}/itinerary")
    public TourDetail replaceItinerary(@PathVariable Long id, @Valid @RequestBody ItineraryRequest r) {
        return tourService.replaceItinerary(id, r);
    }

    @DeleteMapping("/tours/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tourService.delete(id);
    }

    // ---------- Đợt khởi hành ----------

    @GetMapping("/tours/{id}/departures")
    public List<DepartureResponse> departures(@PathVariable Long id) {
        return departureService.listForAdmin(id);
    }

    /** Tạo đợt mới; lịch trình của đợt được sao chép từ lịch trình mẫu hiện tại của tour. */
    @PostMapping("/tours/{id}/departures")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartureResponse createDeparture(@PathVariable Long id, @Valid @RequestBody DepartureRequest r) {
        return departureService.create(id, r);
    }

    @PutMapping("/departures/{id}")
    public DepartureResponse updateDeparture(@PathVariable Long id, @Valid @RequestBody DepartureRequest r) {
        return departureService.update(id, r);
    }

    @GetMapping("/departures/{id}/itinerary")
    public ItineraryResponse departureItinerary(@PathVariable Long id) {
        return departureService.itinerary(id, true);
    }

    @PutMapping("/departures/{id}/itinerary")
    public ItineraryResponse replaceDepartureItinerary(@PathVariable Long id, @Valid @RequestBody ItineraryRequest r) {
        return departureService.replaceItinerary(id, r);
    }

    @DeleteMapping("/departures/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeparture(@PathVariable Long id) {
        departureService.delete(id);
    }
}
