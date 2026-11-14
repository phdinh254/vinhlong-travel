package vn.edu.tvu.vinhlongtour.planner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.domain.GeoPoint;
import vn.edu.tvu.vinhlongtour.catalog.repository.DestinationRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.planner.domain.SavedTrip;
import vn.edu.tvu.vinhlongtour.planner.repository.SavedTripRepository;
import vn.edu.tvu.vinhlongtour.planner.scoring.InterestMatch;
import vn.edu.tvu.vinhlongtour.planner.scoring.Popularity;
import vn.edu.tvu.vinhlongtour.planner.scoring.PreferredRegion;
import vn.edu.tvu.vinhlongtour.planner.scoring.ScoringCriterion;
import vn.edu.tvu.vinhlongtour.planner.solver.IteratedLocalSearch;
import vn.edu.tvu.vinhlongtour.planner.solver.Place;
import vn.edu.tvu.vinhlongtour.planner.solver.Timeline;
import vn.edu.tvu.vinhlongtour.planner.solver.TripProblem;
import vn.edu.tvu.vinhlongtour.planner.solver.TripSolution;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.DayPlan;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.LodgingInfo;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SaveTripRequest;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SavedTripDetail;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SavedTripSummary;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SimilarTour;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.Stop;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.StopType;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SuggestRequest;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.TripPlan;
import vn.edu.tvu.vinhlongtour.tour.domain.DepartureStatus;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository;
import vn.edu.tvu.vinhlongtour.tour.repository.DepartureRepository.TourPriceSummary;
import vn.edu.tvu.vinhlongtour.tour.repository.TourRepository;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourCard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vn.edu.tvu.vinhlongtour.planner.solver.TripProblem.LUNCH;

/**
 * Gợi ý lịch trình: dựng bài toán TOPTW từ các điểm đến đang hoạt động (điểm hấp dẫn theo sở thích khách,
 * giờ mở cửa, ngày đóng cửa, giá vé, thời gian đi đường từ nơi lưu trú), giải bằng {@link IteratedLocalSearch},
 * rồi kèm các tour đang bán giống nhất để khách đặt. Khách đã đăng nhập lưu được lịch trình để xem lại.
 */
@Service
@Transactional(readOnly = true)
public class TripPlannerService {

    /** shortcut: tiền ăn trưa ước tính chung 80.000 đ/người, chưa có danh mục quán ăn; thay khi có dữ liệu nhà hàng. */
    static final long LUNCH_COST_PER_PERSON = 80_000;
    private static final int LUNCH_MINUTES = 60;
    private static final LocalTime LUNCH_FROM = LocalTime.of(11, 0);
    private static final LocalTime LUNCH_LATEST_START = LocalTime.of(13, 30);
    private static final int SIMILAR_TOUR_LIMIT = 3;

    private final DestinationRepository destinations;
    private final TourRepository tours;
    private final DepartureRepository departures;
    private final SavedTripRepository trips;
    private final AccountRepository accounts;
    private final JsonMapper json;
    private final Clock clock;

    public TripPlannerService(DestinationRepository destinations, TourRepository tours, DepartureRepository departures,
                              SavedTripRepository trips, AccountRepository accounts, JsonMapper json, Clock clock) {
        this.destinations = destinations;
        this.tours = tours;
        this.departures = departures;
        this.trips = trips;
        this.accounts = accounts;
        this.json = json;
        this.clock = clock;
    }

    /** Bài toán đã dựng từ yêu cầu của khách: {@code candidates.get(i)} là điểm thứ i của bài toán. */
    record Prepared(TripProblem problem, List<Destination> candidates, LodgingInfo lodging, int children) {}

    public TripPlan suggest(SuggestRequest r) {
        Prepared prepared = prepare(r);
        return toPlan(r, prepared, new IteratedLocalSearch(prepared.problem()).solve());
    }

    Prepared prepare(SuggestRequest r) {
        if (r.startDate().isBefore(LocalDate.now(clock))) {
            throw ApiException.badRequest("INVALID_START_DATE", "Ngày bắt đầu không được ở quá khứ");
        }
        LodgingInfo lodging = lodgingOf(r);
        int children = r.children() == null ? 0 : r.children();
        long lunchCost = LUNCH_COST_PER_PERSON * (r.adults() + children);
        long budget = r.budget() == null ? Long.MAX_VALUE : r.budget();
        if (lunchCost * r.days() > budget) {
            throw ApiException.badRequest("BUDGET_TOO_LOW",
                    "Ngân sách chưa đủ tiền ăn trưa ước tính, cần ít nhất " + lunchCost * r.days() + " đồng");
        }

        List<ScoringCriterion> criteria = criteria(r);
        // Điểm đến không đủ thời gian tham quan trong giờ mở cửa thì không xếp được
        List<Destination> candidates = destinations.findByActiveTrue().stream()
                .filter(d -> latestStart(d) >= minutes(d.getOpeningHours().openTime()))
                .sorted(Comparator.comparing(Destination::getCode))
                .toList();
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(lodging.latitude(), lodging.longitude()));
        candidates.forEach(d -> points.add(d.getLocation()));
        int[][] travel = new int[points.size()][points.size()];
        for (int a = 0; a < points.size(); a++) {
            for (int b = 0; b < points.size(); b++) {
                travel[a][b] = TravelTimes.minutes(points.get(a), points.get(b));
            }
        }
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Destination d = candidates.get(i);
            places.add(new Place(ScoringCriterion.score(criteria, d), d.getVisitMinutes(), minutes(d.getOpeningHours().openTime()),
                    latestStart(d), ticketCost(d, r.adults(), children), i + 1, closedDays(d, r)));
        }
        Place lunch = new Place(0, LUNCH_MINUTES, minutes(LUNCH_FROM), minutes(LUNCH_LATEST_START), lunchCost, Place.HERE, Set.of());
        TripProblem problem = new TripProblem(travel, places, lunch, r.days(),
                minutes(r.pace().dayStart()), minutes(r.pace().dayEnd()), budget);
        return new Prepared(problem, candidates, lodging, children);
    }

    @Transactional
    public SavedTripDetail save(SaveTripRequest r, Long accountId) {
        TripPlan plan = suggest(r.request());
        SavedTrip trip = trips.save(new SavedTrip(accounts.getReferenceById(accountId), r.title().trim(),
                json.writeValueAsString(r.request()), json.writeValueAsString(plan), LocalDateTime.now(clock)));
        return new SavedTripDetail(trip.getId(), trip.getTitle(), trip.getCreatedAt(), r.request(), plan);
    }

    public List<SavedTripSummary> listMine(Long accountId) {
        return trips.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(t -> {
            SuggestRequest r = json.readValue(t.getRequest(), SuggestRequest.class);
            return new SavedTripSummary(t.getId(), t.getTitle(), r.startDate(), r.days(), t.getCreatedAt());
        }).toList();
    }

    /** Lịch giữ nguyên như lúc lưu; riêng tour giống nhất tính lại vì giá và đợt khởi hành thay đổi theo thời gian. */
    public SavedTripDetail getMine(Long accountId, Long id) {
        SavedTrip t = findMine(accountId, id);
        TripPlan plan = json.readValue(t.getPlan(), TripPlan.class);
        Set<Long> visited = plan.dayPlans().stream().flatMap(d -> d.stops().stream())
                .map(Stop::destinationId).filter(Objects::nonNull).collect(Collectors.toSet());
        return new SavedTripDetail(t.getId(), t.getTitle(), t.getCreatedAt(),
                json.readValue(t.getRequest(), SuggestRequest.class), plan.withSimilarTours(similarTours(visited)));
    }

    @Transactional
    public void deleteMine(Long accountId, Long id) {
        trips.delete(findMine(accountId, id));
    }

    private SavedTrip findMine(Long accountId, Long id) {
        return trips.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Không tìm thấy lịch trình"));
    }

    private static LodgingInfo lodgingOf(SuggestRequest r) {
        if (r.lodging() != null) {
            GeoPoint p = r.lodging().location();
            return new LodgingInfo(r.lodging(), r.lodging().displayName(), p.latitude(), p.longitude());
        }
        if (r.lodgingLatitude() == null || r.lodgingLongitude() == null) {
            throw ApiException.badRequest("LODGING_REQUIRED", "Chọn nơi lưu trú gợi ý sẵn hoặc gửi tọa độ nơi lưu trú");
        }
        GeoPoint p = new GeoPoint(r.lodgingLatitude(), r.lodgingLongitude());
        return new LodgingInfo(null, "Nơi lưu trú tự chọn", p.latitude(), p.longitude());
    }

    /** Độ nổi bật luôn tính; sở thích và khu vực chỉ tính khi khách có chọn. */
    private static List<ScoringCriterion> criteria(SuggestRequest r) {
        List<ScoringCriterion> criteria = new ArrayList<>(List.of(new Popularity()));
        if (r.categoryIds() != null && !r.categoryIds().isEmpty()) {
            criteria.add(new InterestMatch(r.categoryIds()));
        }
        if (r.regionIds() != null && !r.regionIds().isEmpty()) {
            criteria.add(new PreferredRegion(r.regionIds()));
        }
        return criteria;
    }

    /** Muộn nhất phải bắt đầu tham quan lúc này để kịp xong trước giờ đóng cửa. */
    private static int latestStart(Destination d) {
        return minutes(d.getOpeningHours().closeTime()) - d.getVisitMinutes();
    }

    /** Chưa rõ giá vé thì tính 0 (giao diện ghi chú theo {@code priceQuality}); chưa rõ giá trẻ em thì tính bằng người lớn. */
    private static long ticketCost(Destination d, int adults, int children) {
        long adult = d.getTicketPriceAdult() == null ? 0 : d.getTicketPriceAdult();
        long child = d.getTicketPriceChild() == null ? adult : d.getTicketPriceChild();
        return adults * adult + children * child;
    }

    private static Set<Integer> closedDays(Destination d, SuggestRequest r) {
        Set<Integer> closed = new HashSet<>();
        for (int k = 0; k < r.days(); k++) {
            if (d.getClosedDays().contains(r.startDate().plusDays(k).getDayOfWeek())) {
                closed.add(k);
            }
        }
        return closed;
    }

    private TripPlan toPlan(SuggestRequest r, Prepared prepared, TripSolution s) {
        TripProblem problem = prepared.problem();
        List<DayPlan> days = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        int totalTravel = 0;
        for (int day = 0; day < s.routes().size(); day++) {
            List<Integer> route = s.routes().get(day);
            Timeline t = s.timelines().get(day);
            List<Stop> stops = new ArrayList<>();
            long dayCost = 0;
            int dayTravel = t.returnTravel();
            for (int i = 0; i < route.size(); i++) {
                Place p = problem.node(route.get(i));
                LocalTime arrival = time(t.arrival()[i]);
                LocalTime start = time(t.start()[i]);
                LocalTime leave = time(t.start()[i] + p.serviceMinutes());
                dayCost += p.cost();
                dayTravel += t.travel()[i];
                if (route.get(i) == LUNCH) {
                    stops.add(new Stop(StopType.LUNCH, null, null, "Ăn trưa", null, null, 0, arrival, start, leave,
                            t.waiting()[i], 0, p.cost(), null, null));
                } else {
                    Destination d = prepared.candidates().get(route.get(i));
                    visited.add(d.getId());
                    stops.add(new Stop(StopType.VISIT, d.getId(), d.getCode(), d.getName(), d.getLocation().latitude(),
                            d.getLocation().longitude(), t.travel()[i], arrival, start, leave, t.waiting()[i], p.score(), p.cost(),
                            d.getHoursQuality(), d.getPriceQuality()));
                }
            }
            totalTravel += dayTravel;
            days.add(new DayPlan(day + 1, r.startDate().plusDays(day), r.pace().dayStart(), time(t.returnAt()),
                    t.returnTravel(), dayTravel, dayCost, stops));
        }
        return new TripPlan(r.startDate(), r.days(), r.pace(), prepared.lodging(), r.adults(), prepared.children(), r.budget(),
                s.score(), s.cost(), totalTravel, days, similarTours(visited));
    }

    /** Tối đa 3 tour đang bán có lịch trình mẫu trùng điểm đến nhiều nhất (hệ số Jaccard), kèm giá từ và ngày đi gần nhất. */
    private List<SimilarTour> similarTours(Set<Long> visited) {
        if (visited.isEmpty()) {
            return List.of();
        }
        record Match(Tour tour, double similarity) {}
        List<Match> matches = tours.findByStatus(TourStatus.ON_SALE).stream()
                .map(tour -> new Match(tour, jaccard(visited, destinationIds(tour))))
                .filter(m -> m.similarity() > 0)
                .sorted(Comparator.comparingDouble(Match::similarity).reversed().thenComparing(m -> m.tour().getId()))
                .limit(SIMILAR_TOUR_LIMIT)
                .toList();
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<Long, TourPriceSummary> prices = departures.summarize(matches.stream().map(m -> m.tour().getId()).toList(),
                        DepartureStatus.OPEN, LocalDateTime.now(clock)).stream()
                .collect(Collectors.toMap(TourPriceSummary::getTourId, Function.identity()));
        return matches.stream()
                .map(m -> new SimilarTour(Math.round(m.similarity() * 100) / 100.0, TourCard.from(m.tour(), prices.get(m.tour().getId()))))
                .toList();
    }

    private static Set<Long> destinationIds(Tour tour) {
        if (tour.getItinerary() == null) {
            return Set.of();
        }
        return tour.getItinerary().getDays().stream().flatMap(d -> d.getActivities().stream())
                .filter(a -> a.getDestination() != null)
                .map(a -> a.getDestination().getId())
                .collect(Collectors.toSet());
    }

    static double jaccard(Set<Long> a, Set<Long> b) {
        Set<Long> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) {
            return 0;
        }
        return (double) a.stream().filter(b::contains).count() / union.size();
    }

    private static int minutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private static LocalTime time(int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60);
    }
}
