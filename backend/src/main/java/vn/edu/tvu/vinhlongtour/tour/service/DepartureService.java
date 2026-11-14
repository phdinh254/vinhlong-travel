package vn.edu.tvu.vinhlongtour.tour.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.repository.PassengerTypeRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.PickupPointRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.Lookups;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DeparturePickup;
import vn.edu.tvu.vinhlongtour.tour.domain.DeparturePrice;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;
import vn.edu.tvu.vinhlongtour.tour.repository.TourRepository;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryResponse;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.PickupLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.PriceLine;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ đợt khởi hành: khách xem các đợt còn nhận khách của tour đang bán;
 * admin tạo đợt (tự sao chép lịch trình mẫu), sửa lịch, sức chứa, giá, điểm đón, lịch trình riêng.
 */
@Service
@Transactional
public class DepartureService {

    private final DepartureRepository departures;
    private final TourRepository tours;
    private final PassengerTypeRepository passengerTypes;
    private final PickupPointRepository pickupPoints;
    private final ItineraryPlans itineraryPlans;
    private final Clock clock;

    public DepartureService(DepartureRepository departures, TourRepository tours, PassengerTypeRepository passengerTypes,
                            PickupPointRepository pickupPoints, ItineraryPlans itineraryPlans, Clock clock) {
        this.departures = departures;
        this.tours = tours;
        this.passengerTypes = passengerTypes;
        this.pickupPoints = pickupPoints;
        this.itineraryPlans = itineraryPlans;
        this.clock = clock;
    }

    /** Đợt khách xem được: tour đang bán, đợt đang mở, chưa quá hạn nhận khách (đợt hết chỗ vẫn hiện, không đặt được). */
    @Transactional(readOnly = true)
    public List<DepartureResponse> listForCustomer(Long tourId) {
        Tour tour = tours.findById(tourId).filter(Tour::isOnSale)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Tour không tồn tại"));
        LocalDateTime now = now();
        return departures.findByTourIdAndStatusAndBookingDeadlineAfterOrderByDepartureAt(tour.getId(), DepartureStatus.OPEN, now)
                .stream().map(d -> DepartureResponse.from(d, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartureResponse> listForAdmin(Long tourId) {
        LocalDateTime now = now();
        return departures.findByTourIdOrderByDepartureAt(tourId).stream().map(d -> DepartureResponse.from(d, now)).toList();
    }

    @Transactional(readOnly = true)
    public DepartureResponse get(Long id, boolean admin) {
        return DepartureResponse.from(find(id, admin), now());
    }

    @Transactional(readOnly = true)
    public ItineraryResponse itinerary(Long id, boolean admin) {
        return ItineraryResponse.from(find(id, admin).getItinerary());
    }

    public DepartureResponse create(Long tourId, DepartureRequest r) {
        Tour tour = tours.findById(tourId).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Tour không tồn tại"));
        Departure d = new Departure(tour, r.departureAt(), r.returnAt(), r.bookingDeadline(), r.capacity(), now());
        if (departures.existsByCode(d.getCode())) {
            throw ApiException.conflict("DEPARTURE_EXISTS", "Tour đã có đợt khởi hành lúc " + r.departureAt());
        }
        apply(d, r);
        return DepartureResponse.from(departures.save(d), now());
    }

    public DepartureResponse update(Long id, DepartureRequest r) {
        Departure d = find(id, true);
        d.schedule(r.departureAt(), r.returnAt(), r.bookingDeadline());
        d.changeCapacity(r.capacity());
        apply(d, r);
        return DepartureResponse.from(d, now());
    }

    public ItineraryResponse replaceItinerary(Long id, ItineraryRequest r) {
        Departure d = find(id, true);
        d.replaceItinerary(itineraryPlans.from(r));
        return ItineraryResponse.from(d.getItinerary());
    }

    public void delete(Long id) {
        Departure d = find(id, true);
        d.ensureNoBookings();
        departures.delete(d);
        departures.flush();
    }

    private void apply(Departure d, DepartureRequest r) {
        var typeById = Lookups.all(passengerTypes, r.prices().stream().map(PriceLine::passengerTypeId).toList(),
                        "PASSENGER_TYPE_NOT_FOUND", "Có loại khách không tồn tại")
                .stream().collect(Collectors.toMap(t -> t.getId(), Function.identity()));
        d.setPrices(r.prices().stream().map(p -> new DeparturePrice(typeById.get(p.passengerTypeId()), p.price())).toList(),
                passengerTypes.findAll());
        List<PickupLine> pickups = r.pickups() == null ? List.of() : r.pickups();
        var pointById = Lookups.all(pickupPoints, pickups.stream().map(PickupLine::pickupPointId).toList(),
                        "PICKUP_POINT_NOT_FOUND", "Có điểm đón không tồn tại")
                .stream().collect(Collectors.toMap(p -> p.getId(), Function.identity()));
        d.setPickups(pickups.stream().map(p -> new DeparturePickup(pointById.get(p.pickupPointId()), p.pickupTime(), p.fee())).toList());
        if (r.status() != null) {
            d.changeStatus(r.status());
        }
    }

    /** Khách chỉ xem được đợt của tour đang bán và chưa bị hủy. */
    private Departure find(Long id, boolean admin) {
        return departures.findById(id)
                .filter(d -> admin || (d.getTour().isOnSale() && d.getStatus() != DepartureStatus.CANCELLED))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Đợt khởi hành không tồn tại"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
