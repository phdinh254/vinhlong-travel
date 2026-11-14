package vn.edu.tvu.vinhlongtour.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.auth.domain.Role;
import vn.edu.tvu.vinhlongtour.auth.service.AccountService;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.RegisterRequest;
import vn.edu.tvu.vinhlongtour.booking.domain.PaymentMethod;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerInput;
import vn.edu.tvu.vinhlongtour.catalog.domain.Amenity;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.domain.NamedItem;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.SurchargeMethod;
import vn.edu.tvu.vinhlongtour.catalog.repository.DestinationRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.PassengerTypeRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.RegionRepository;
import vn.edu.tvu.vinhlongtour.catalog.repository.TourCategoryRepository;
import vn.edu.tvu.vinhlongtour.catalog.service.CancellationPolicyService;
import vn.edu.tvu.vinhlongtour.catalog.service.NamedItemService;
import vn.edu.tvu.vinhlongtour.catalog.service.PickupPointService;
import vn.edu.tvu.vinhlongtour.catalog.service.SurchargeService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PenaltyTierDto;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PickupPointRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeRequest;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleCategory;
import vn.edu.tvu.vinhlongtour.content.domain.ArticleStatus;
import vn.edu.tvu.vinhlongtour.content.domain.SettingKey;
import vn.edu.tvu.vinhlongtour.content.service.ArticleService;
import vn.edu.tvu.vinhlongtour.content.service.ContactService;
import vn.edu.tvu.vinhlongtour.content.service.SiteSettingService;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ArticleRequest;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.payment.PaymentService;
import vn.edu.tvu.vinhlongtour.tour.domain.TourStatus;
import vn.edu.tvu.vinhlongtour.tour.repository.TourRepository;
import vn.edu.tvu.vinhlongtour.tour.service.DepartureService;
import vn.edu.tvu.vinhlongtour.tour.service.TourService;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ActivityLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.AmenityLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DayLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.DepartureRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ImageLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ImagesRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.ItineraryRequest;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.PickupLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.PriceLine;
import vn.edu.tvu.vinhlongtour.tour.web.TourDtos.TourRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nạp dữ liệu demo khi chạy ở profile {@code dev} và CSDL chưa có tour nào: 4 tour trên các điểm đến thật,
 * đợt khởi hành trong tháng tới, chính sách hủy, điểm đón, phụ thu, một khách mẫu, vài đơn ở các trạng thái
 * khác nhau, bài viết và cấu hình trang. Mọi thứ đi qua service nên được kiểm tra y như khi admin nhập tay.
 * Không bao giờ chạy ở môi trường thật (không bật profile {@code dev}).
 */
@Component
@Profile("dev")
class DemoDataLoader implements ApplicationRunner {

    static final String DEMO_CUSTOMER_EMAIL = "khach@demo.vn";
    static final String DEMO_CUSTOMER_PASSWORD = "Demo@12345";

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final TourRepository tourRepo;
    private final RegionRepository regions;
    private final TourCategoryRepository categories;
    private final PassengerTypeRepository passengerTypes;
    private final DestinationRepository destinations;
    private final AccountRepository accountRepo;
    private final NamedItemService<Amenity> amenities;
    private final PickupPointService pickupPoints;
    private final SurchargeService surcharges;
    private final CancellationPolicyService policies;
    private final TourService tours;
    private final DepartureService departures;
    private final AccountService accounts;
    private final BookingService bookings;
    private final PaymentService payments;
    private final ArticleService articles;
    private final SiteSettingService settings;
    private final ContactService contacts;
    private final Clock clock;

    DemoDataLoader(TourRepository tourRepo, RegionRepository regions, TourCategoryRepository categories,
                   PassengerTypeRepository passengerTypes, DestinationRepository destinations, AccountRepository accountRepo,
                   NamedItemService<Amenity> amenities, PickupPointService pickupPoints, SurchargeService surcharges,
                   CancellationPolicyService policies, TourService tours, DepartureService departures, AccountService accounts,
                   BookingService bookings, PaymentService payments, ArticleService articles, SiteSettingService settings,
                   ContactService contacts, Clock clock) {
        this.tourRepo = tourRepo;
        this.regions = regions;
        this.categories = categories;
        this.passengerTypes = passengerTypes;
        this.destinations = destinations;
        this.accountRepo = accountRepo;
        this.amenities = amenities;
        this.pickupPoints = pickupPoints;
        this.surcharges = surcharges;
        this.policies = policies;
        this.tours = tours;
        this.departures = departures;
        this.accounts = accounts;
        this.bookings = bookings;
        this.payments = payments;
        this.articles = articles;
        this.settings = settings;
        this.contacts = contacts;
        this.clock = clock;
    }

    /** Một tour demo: mỗi ngày là danh sách (giờ, hoạt động, mã điểm đến hoặc null). */
    private record DemoTour(String code, String name, String summary, int days, Set<String> regionNames, Set<String> categoryNames,
                            List<String> images, List<List<String[]>> plan, long adultPrice, long childPrice,
                            boolean featured, boolean withSingleRoom) {}

    @Override
    public void run(ApplicationArguments args) {
        if (tourRepo.count() > 0) {
            log.info("Đã có tour trong CSDL, bỏ qua nạp dữ liệu demo");
            return;
        }
        accounts.createAdminIfMissing();
        Long adminId = accountRepo.findFirstByRoleOrderByIdAsc(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Profile dev cần tài khoản admin (đặt ADMIN_PASSWORD)")).getId();
        LocalDate today = LocalDate.now(clock);

        policies.create(new CancellationPolicyRequest("Chính sách hủy tiêu chuẩn", today,
                "Tính theo số ngày từ lúc báo hủy tới ngày khởi hành", List.of(
                new PenaltyTierDto(0, 2, 100), new PenaltyTierDto(3, 6, 50),
                new PenaltyTierDto(7, 14, 20), new PenaltyTierDto(15, 365, 0))));
        Long pickupVinhLong = pickupPoints.create(new PickupPointRequest("Văn phòng VinhLong Tour",
                "Đường 2/9, phường 1, TP. Vĩnh Long (địa chỉ demo)", true)).id();
        Long pickupBenTre = pickupPoints.create(new PickupPointRequest("Bến xe khách Bến Tre", "Phường Phú Khương, TP. Bến Tre", true)).id();
        Long singleRoom = surcharges.create(new SurchargeRequest("Phụ thu phòng đơn", SurchargeMethod.PER_UNIT, 350_000L,
                "Cho khách muốn ở riêng một phòng", true)).id();
        List<AmenityLine> amenityLines = new ArrayList<>();
        for (String name : List.of("Xe đưa đón", "Hướng dẫn viên", "Bữa trưa", "Vé tham quan")) {
            amenityLines.add(new AmenityLine(amenities.create(name).id(), true));
        }
        amenityLines.add(new AmenityLine(amenities.create("Chi phí cá nhân").id(), false));

        Map<String, Long> regionIds = ids(regions.findAll());
        Map<String, Long> categoryIds = ids(categories.findAll());
        Map<String, Long> destinationIds = destinations.findAll().stream().collect(Collectors.toMap(Destination::getCode, Destination::getId));
        List<PassengerType> types = passengerTypes.findAll();

        List<DemoTour> demo = List.of(
                new DemoTour("VLANBINH1N", "Vĩnh Long: Văn Thánh Miếu – cù lao An Bình – làng gốm Mang Thít",
                        "Một ngày miệt vườn: di tích, đi thuyền vào vườn trái cây, xem lò gạch gốm đỏ", 1,
                        Set.of("Vĩnh Long (cũ)"), Set.of("Sinh thái", "Văn hóa", "Làng nghề"),
                        List.of("/images/an-binh.jpg", "/images/van-thanh-mieu.jpg", "/images/mang-thit.jpg"),
                        List.of(List.of(
                                new String[]{"07:30", "Tham quan Văn Thánh Miếu", "VL01"},
                                new String[]{"09:00", "Đi thuyền sang cù lao An Bình, vườn trái cây", "VL03"},
                                new String[]{"11:30", "Ăn trưa đặc sản miệt vườn", null},
                                new String[]{"14:00", "Làng gạch gốm Mang Thít", "VL06"})),
                        650_000, 450_000, true, false),
                new DemoTour("BTXUDUA1N", "Bến Tre: Cồn Phụng – Đạo Dừa – làng hoa kiểng Cái Mơn",
                        "Xứ dừa trong một ngày: nghề kẹo dừa, đờn ca tài tử, làng hoa kiểng", 1,
                        Set.of("Bến Tre (cũ)"), Set.of("Sinh thái", "Làng nghề"),
                        List.of("/images/con-phung.jpg", "/images/keo-dua.jpg", "/images/song-nuoc.jpg"),
                        List.of(List.of(
                                new String[]{"08:00", "Khu du lịch Cồn Phụng, xem làm kẹo dừa", "BT01"},
                                new String[]{"10:00", "Di tích Đạo Dừa", "BT02"},
                                new String[]{"11:30", "Ăn trưa trên cồn", null},
                                new String[]{"14:00", "Làng hoa kiểng Cái Mơn", "BT13"})),
                        700_000, 500_000, true, false),
                new DemoTour("TVKHMER2N", "Trà Vinh: văn hóa Khmer và biển Ba Động 2 ngày 1 đêm",
                        "Chùa Khmer, Ao Bà Om, bảo tàng Khmer và biển Ba Động", 2,
                        Set.of("Trà Vinh (cũ)"), Set.of("Văn hóa", "Tâm linh", "Biển"),
                        List.of("/images/ao-ba-om.jpg", "/images/chua-ang.jpg", "/images/chua-hang.jpg"),
                        List.of(List.of(
                                        new String[]{"08:00", "Dạo Ao Bà Om", "TV01"},
                                        new String[]{"09:30", "Chùa Âng", "TV02"},
                                        new String[]{"14:00", "Bảo tàng Văn hóa Dân tộc Khmer", "TV03"}),
                                List.of(
                                        new String[]{"07:30", "Chùa Hang (chùa Cò)", "TV04"},
                                        new String[]{"10:00", "Tắm biển Ba Động", "TV05"})),
                        1_590_000, 1_100_000, false, true),
                new DemoTour("MIENTAY3N", "Sông nước ba miền: Vĩnh Long – Bến Tre – Trà Vinh 3 ngày 2 đêm",
                        "Chợ nổi Trà Ôn, cù lao An Bình, xứ dừa Bến Tre, chùa Khmer Trà Vinh", 3,
                        Set.of("Vĩnh Long (cũ)", "Bến Tre (cũ)", "Trà Vinh (cũ)"), Set.of("Sinh thái", "Văn hóa", "Chợ & ẩm thực"),
                        List.of("/images/cho-noi.jpg", "/images/kenh-rach.jpg", "/images/homestay.jpg"),
                        List.of(
                                List.of(new String[]{"06:00", "Chợ nổi Trà Ôn", "VL07"},
                                        new String[]{"13:30", "Cù lao An Bình, nghỉ homestay", "VL03"}),
                                List.of(new String[]{"08:00", "Khu du lịch Cồn Phụng", "BT01"},
                                        new String[]{"10:30", "Di tích Đạo Dừa", "BT02"}),
                                List.of(new String[]{"08:00", "Ao Bà Om", "TV01"},
                                        new String[]{"10:00", "Chùa Âng", "TV02"})),
                        2_890_000, 1_990_000, true, true));

        List<List<Long>> departureIds = new ArrayList<>();
        for (DemoTour t : demo) {
            Set<Long> tourRegions = t.regionNames().stream().map(regionIds::get).collect(Collectors.toSet());
            Set<Long> tourCategories = t.categoryNames().stream().map(categoryIds::get).collect(Collectors.toSet());
            TourRequest request = new TourRequest(t.code(), t.name(), t.summary(), t.summary(), t.days(), t.days() - 1,
                    tourRegions, tourCategories, amenityLines, t.withSingleRoom() ? Set.of(singleRoom) : Set.of(), t.featured(), null);
            Long tourId = tours.create(request).id();
            tours.replaceImages(tourId, new ImagesRequest(t.images().stream().map(url -> new ImageLine(url, null, null)).toList()));
            List<DayLine> days = new ArrayList<>();
            for (int d = 0; d < t.plan().size(); d++) {
                days.add(new DayLine("Ngày " + (d + 1), null, t.plan().get(d).stream()
                        .map(a -> new ActivityLine(LocalTime.parse(a[0]), a[1], null, a[2] == null ? null : destinationIds.get(a[2])))
                        .toList()));
            }
            tours.replaceItinerary(tourId, new ItineraryRequest(days));
            tours.update(tourId, new TourRequest(t.code(), t.name(), t.summary(), t.summary(), t.days(), t.days() - 1,
                    tourRegions, tourCategories, amenityLines, request.surchargeIds(), t.featured(), TourStatus.ON_SALE));

            List<PriceLine> prices = types.stream().map(p -> new PriceLine(p.getId(),
                    p.getAgeRange().minAge() >= 12 ? t.adultPrice() : p.getAgeRange().minAge() >= 5 ? t.childPrice() : 0L)).toList();
            Long pickup = t.code().startsWith("BT") ? pickupBenTre : pickupVinhLong;
            List<Long> created = new ArrayList<>();
            for (int daysAhead : new int[]{5, 12, 26}) {
                LocalDateTime start = today.plusDays(daysAhead).atTime(7, 0);
                created.add(departures.create(tourId, new DepartureRequest(start, start.plusDays(t.days() - 1).plusHours(10),
                        start.minusDays(1), 20, null, prices, List.of(new PickupLine(pickup, LocalTime.of(6, 30), 0L)))).id());
            }
            departureIds.add(created);
        }

        // Khách mẫu và 3 đơn ở 3 trạng thái: đã trả đủ, đã cọc, chờ thanh toán (sẽ tự hết hạn sau 30 phút)
        Long customerId = accounts.register(new RegisterRequest(DEMO_CUSTOMER_EMAIL, DEMO_CUSTOMER_PASSWORD,
                "Nguyễn Văn Khách", "0901234567")).account().id();
        BookingDetail paid = bookings.create(new BookingRequest(departureIds.get(0).get(0), "Nguyễn Văn Khách", "0901234567",
                DEMO_CUSTOMER_EMAIL, "Gia đình có trẻ nhỏ", List.of(
                passenger("Nguyễn Văn Khách", "1985-03-12", pickupVinhLong),
                passenger("Trần Thị Mai", "1987-06-20", pickupVinhLong),
                passenger("Nguyễn Minh An", "2017-09-01", pickupVinhLong)), List.of()), customerId);
        payments.recordManualPayment(paid.id(), new ManualPaymentRequest(paid.totalAmount(), PaymentMethod.BANK_TRANSFER,
                "CK-DEMO-001", null, "Khách chuyển khoản đủ"), adminId);
        BookingDetail deposited = bookings.create(new BookingRequest(departureIds.get(2).get(0), "Lê Hoàng Phúc", "0912345678",
                null, null, List.of(passenger("Lê Hoàng Phúc", "1979-11-02", null)), List.of()), null);
        payments.recordManualPayment(deposited.id(), new ManualPaymentRequest(deposited.minimumFirstPayment(), PaymentMethod.CASH,
                null, null, "Đặt cọc tại văn phòng"), adminId);
        bookings.create(new BookingRequest(departureIds.get(1).get(1), "Phạm Ngọc Hân", "0987654321", null, null,
                List.of(passenger("Phạm Ngọc Hân", "1995-05-15", null), passenger("Võ Thanh Tâm", "1994-08-30", null)), List.of()), null);

        loadContent();
        log.info("Đã nạp dữ liệu demo: {} tour, khách mẫu {} / {}", demo.size(), DEMO_CUSTOMER_EMAIL, DEMO_CUSTOMER_PASSWORD);
    }

    private void loadContent() {
        articles.create(new ArticleRequest("Lễ hội Ok Om Bok của đồng bào Khmer Trà Vinh", null,
                "Đêm rằm tháng 10 âm lịch, Ao Bà Om rộn ràng đua ghe Ngo, thả đèn nước và cúng trăng.",
                "<p>Ok Om Bok là lễ cúng trăng của người Khmer Nam Bộ, tổ chức vào rằm tháng 10 âm lịch để tạ ơn mùa màng.</p>"
                        + "<p>Ở Trà Vinh, lễ hội diễn ra quanh Ao Bà Om với đua ghe Ngo, thả đèn gió, đèn nước và các đêm văn nghệ.</p>"
                        + "<p>Nên đặt chỗ nghỉ sớm vì khách đổ về rất đông những ngày này.</p>",
                "/images/ao-ba-om.jpg", ArticleCategory.EVENT, ArticleStatus.PUBLISHED));
        articles.create(new ArticleRequest("Kinh nghiệm đi cù lao An Bình trong một ngày", null,
                "Đi giờ nào, ăn gì, mang gì khi về miệt vườn Vĩnh Long.",
                "<p>Cù lao An Bình nằm giữa sông Cổ Chiên, đi đò từ trung tâm Vĩnh Long chỉ vài phút.</p>"
                        + "<ul><li>Nên đi sáng sớm để mát và kịp vào vườn trái cây.</li>"
                        + "<li>Mang nón, kem chống nắng; đường làng hẹp, đi xe đạp là thú vị nhất.</li>"
                        + "<li>Thử cá tai tượng chiên xù và bánh xèo miệt vườn.</li></ul>",
                "/images/an-binh.jpg", ArticleCategory.TIPS, ArticleStatus.PUBLISHED));
        articles.create(new ArticleRequest("Nghề làm kẹo dừa ở Bến Tre", null,
                "Từ trái dừa tới viên kẹo: nghề thủ công nuôi sống nhiều làng quê xứ dừa.",
                "<p>Kẹo dừa được nấu từ nước cốt dừa, mạch nha và đường, khuấy liên tục trên bếp lửa rồi cán, cắt, gói bằng tay.</p>"
                        + "<p>Khách ghé các cơ sở trên cồn có thể xem tận nơi và tự tay gói thử vài viên.</p>",
                "/images/keo-dua.jpg", ArticleCategory.CULTURE, ArticleStatus.PUBLISHED));
        articles.create(new ArticleRequest("Bản nháp: lịch tour Tết 2027", null, null, "<p>Đang soạn.</p>", null,
                ArticleCategory.NEWS, ArticleStatus.DRAFT));

        settings.update(Map.of(
                SettingKey.HOTLINE, "0270 3 822 222",
                SettingKey.EMAIL, "lienhe@vinhlongtour.vn",
                SettingKey.ADDRESS, "Đường 2/9, phường 1, TP. Vĩnh Long (địa chỉ demo)",
                SettingKey.SUPPORT_HOURS, "Hỗ trợ 7:00 – 21:00",
                SettingKey.MAP_EMBED_URL, "https://www.google.com/maps?q=10.2537,105.9722&output=embed",
                SettingKey.ABOUT_HTML, "<p>VinhLong Tour tổ chức tour trong ngày và nhiều ngày ở Vĩnh Long, Bến Tre, Trà Vinh.</p>",
                SettingKey.TERMS_HTML, "<p>Khách đặt cọc tối thiểu 30% để giữ chỗ; trả đủ trước ngày khởi hành.</p>",
                SettingKey.PRIVACY_HTML, "<p>Thông tin khách chỉ dùng để liên hệ và làm thủ tục cho chuyến đi.</p>",
                SettingKey.CANCELLATION_HTML, "<p>Phí hủy tính theo số ngày trước ngày khởi hành, xem bảng mức phạt bên dưới.</p>"));
        contacts.submit(new ContactRequest("Đặng Thu Trang", "0933111222", null, "Tư vấn tour cho nhóm",
                "Nhóm 12 người muốn đi Trà Vinh 2 ngày vào cuối tháng, có người lớn tuổi."));
    }

    private static PassengerInput passenger(String name, String birthDate, Long pickupPointId) {
        return new PassengerInput(name, LocalDate.parse(birthDate), null, null, null, pickupPointId);
    }

    private static Map<String, Long> ids(List<? extends NamedItem> items) {
        return items.stream().collect(Collectors.toMap(NamedItem::getName, NamedItem::getId, (a, b) -> a));
    }
}
