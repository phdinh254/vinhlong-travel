package vn.edu.tvu.vinhlongtour.tour.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.tvu.vinhlongtour.catalog.domain.AgeRange;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra quy tắc của tour, lịch trình và đợt khởi hành mà không cần CSDL.
 */
class TourDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 9, 0);

    private static Itinerary.DayPlan day(String title, Itinerary.ActivityPlan... activities) {
        return new Itinerary.DayPlan(title, null, List.of(activities));
    }

    private static Itinerary.ActivityPlan at(int hour, String title) {
        return new Itinerary.ActivityPlan(LocalTime.of(hour, 0), title, null, null);
    }

    private static Tour twoDayTour() {
        return new Tour("vl2n", "Vĩnh Long 2 ngày", 2, 1);
    }

    // ---------- Ảnh ----------

    @Test
    void firstImageBecomesCoverWhenNoneChosen() {
        Tour tour = twoDayTour();
        tour.replaceImages(List.of(new TourImage("/uploads/a.jpg", null, false), new TourImage("/uploads/b.jpg", null, false)));

        assertThat(tour.coverImageUrl()).isEqualTo("/uploads/a.jpg");
        assertThat(tour.getImages()).filteredOn(TourImage::cover).hasSize(1);
    }

    @Test
    void twoCoversAreRejected() {
        assertCode(() -> twoDayTour().replaceImages(List.of(
                new TourImage("/uploads/a.jpg", null, true), new TourImage("/uploads/b.jpg", null, true))), "MULTIPLE_COVERS");
    }

    // ---------- Mở bán ----------

    @Test
    void tourGoesOnSaleOnlyWithCoverAndFullItinerary() {
        Tour tour = twoDayTour();
        assertCode(() -> tour.changeStatus(TourStatus.ON_SALE), "TOUR_INCOMPLETE");          // chưa có ảnh

        tour.replaceImages(List.of(new TourImage("/uploads/a.jpg", null, true)));
        tour.replaceItinerary(List.of(day("Ngày 1")));
        assertCode(() -> tour.changeStatus(TourStatus.ON_SALE), "TOUR_INCOMPLETE");          // lịch trình 1 ngày, tour 2 ngày

        tour.replaceItinerary(List.of(day("Ngày 1"), day("Ngày 2")));
        tour.changeStatus(TourStatus.ON_SALE);
        assertThat(tour.isOnSale()).isTrue();

        // Đang bán mà xóa hết ảnh thì bị chặn
        assertCode(() -> tour.replaceImages(List.of()), "TOUR_INCOMPLETE");
    }

    // ---------- Lịch trình ----------

    @Test
    void copiedItineraryIsIndependentFromTheOriginal() {
        Itinerary template = new Itinerary();
        template.replaceDays(List.of(day("Cù lao An Bình", at(8, "Đi thuyền"), at(11, "Ăn trưa"))));

        Itinerary copy = template.copy();
        template.replaceDays(List.of(day("Đã đổi")));

        assertThat(copy.getDays()).hasSize(1);
        assertThat(copy.getDays().getFirst().getTitle()).isEqualTo("Cù lao An Bình");
        assertThat(copy.getDays().getFirst().getActivities()).extracting(ItineraryActivity::getTitle).containsExactly("Đi thuyền", "Ăn trưa");
    }

    @Test
    void activitiesMustBeInTimeOrder() {
        assertCode(() -> new Itinerary().replaceDays(List.of(day("Ngày 1", at(11, "Ăn trưa"), at(8, "Đi thuyền")))), "INVALID_ITINERARY");
    }

    // ---------- Đợt khởi hành ----------

    private static Departure departure(Tour tour) {
        return new Departure(tour, NOW.plusDays(10), NOW.plusDays(11), NOW.plusDays(9), 20, NOW);
    }

    @Test
    void departureCopiesTourItineraryAndGetsCodeFromDate() {
        Tour tour = twoDayTour();
        tour.replaceItinerary(List.of(day("Ngày 1"), day("Ngày 2")));

        Departure d = departure(tour);

        assertThat(d.getCode()).isEqualTo("VL2N-261124-0900");
        assertThat(d.getItinerary()).isNotSameAs(tour.getItinerary());
        assertThat(d.getItinerary().dayCount()).isEqualTo(2);
        assertThat(d.seatsLeft()).isEqualTo(20);
    }

    @Test
    void departureScheduleMustMakeSense() {
        Tour tour = twoDayTour();
        assertCode(() -> new Departure(tour, NOW.minusDays(1), NOW.plusDays(1), NOW.minusDays(2), 20, NOW), "DEPARTURE_IN_PAST");
        assertCode(() -> new Departure(tour, NOW.plusDays(5), NOW.plusDays(4), NOW.plusDays(3), 20, NOW), "INVALID_SCHEDULE");
        assertCode(() -> new Departure(tour, NOW.plusDays(5), NOW.plusDays(6), NOW.plusDays(5).plusHours(1), 20, NOW), "INVALID_SCHEDULE");
        assertCode(() -> new Departure(tour, NOW.plusDays(5), NOW.plusDays(6), NOW.plusDays(4), 0, NOW), "INVALID_CAPACITY");
    }

    @Test
    void everyPassengerTypeNeedsExactlyOnePrice() {
        PassengerType adult = type(1L, "Người lớn", 12, 120);
        PassengerType child = type(2L, "Trẻ em", 0, 11);
        Departure d = departure(twoDayTour());

        assertCode(() -> d.setPrices(List.of(new DeparturePrice(adult, 900_000)), List.of(adult, child)), "MISSING_PRICE");
        assertCode(() -> d.setPrices(List.of(new DeparturePrice(adult, 900_000), new DeparturePrice(adult, 800_000), new DeparturePrice(child, 1)),
                List.of(adult, child)), "DUPLICATE_PRICE");
        d.setPrices(List.of(new DeparturePrice(adult, 900_000), new DeparturePrice(child, 450_000)), List.of(adult, child));
        assertThat(d.getPrices()).hasSize(2);
    }

    @Test
    void departureIsBookableOnlyWhenOpenOnSaleAndBeforeDeadline() {
        Tour tour = twoDayTour();
        tour.replaceImages(List.of(new TourImage("/uploads/a.jpg", null, true)));
        tour.replaceItinerary(List.of(day("Ngày 1"), day("Ngày 2")));
        Departure d = departure(tour);

        assertThat(d.isBookable(NOW)).isFalse();                 // tour còn nháp
        tour.changeStatus(TourStatus.ON_SALE);
        assertThat(d.isBookable(NOW)).isTrue();
        assertThat(d.isBookable(NOW.plusDays(9))).isFalse();     // đúng hạn chót thì đã hết nhận
        d.changeStatus(DepartureStatus.CLOSED);
        assertThat(d.isBookable(NOW)).isFalse();
    }

    private static PassengerType type(Long id, String name, int min, int max) {
        PassengerType t = new PassengerType(name, new AgeRange(min, max), 1);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
