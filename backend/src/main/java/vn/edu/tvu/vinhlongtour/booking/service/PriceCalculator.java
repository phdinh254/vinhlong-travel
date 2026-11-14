package vn.edu.tvu.vinhlongtour.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.booking.domain.Booking;
import vn.edu.tvu.vinhlongtour.booking.domain.BookingSurcharge;
import vn.edu.tvu.vinhlongtour.booking.domain.Passenger;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerInput;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.SurchargeSelection;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DeparturePickup;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tính giá một đơn từ đợt khởi hành, danh sách hành khách và phụ thu khách chọn:
 * <ul>
 *   <li>Tuổi tính đến <b>ngày khởi hành</b> (không phải hôm nay), chọn loại khách theo khoảng tuổi.</li>
 *   <li>Đơn phải có ít nhất một hành khách từ {@value #ADULT_AGE} tuổi.</li>
 *   <li>Phí đón tính theo từng hành khách chọn điểm đón; phụ thu tính theo cách tính của từng khoản.</li>
 *   <li>Tiền cọc tối thiểu = phần trăm cấu hình × tổng tiền, làm tròn lên hàng nghìn.</li>
 * </ul>
 * Không đọc/ghi CSDL nên kiểm thử được độc lập.
 */
@Component
public class PriceCalculator {

    public static final int ADULT_AGE = Booking.ADULT_AGE;

    private final int depositPercent;

    @Autowired
    public PriceCalculator(AppProperties props) {
        this(props.booking().depositPercent());
    }

    PriceCalculator(int depositPercent) {
        this.depositPercent = depositPercent;
    }

    /** Kết quả tính giá; {@code passengers} và {@code surcharges} dùng luôn để tạo đơn. */
    public record Quote(List<PassengerLine> passengers, List<BookingSurcharge> surcharges,
                        long ticketTotal, long pickupTotal, long surchargeTotal, long total, int seats, long deposit) {}

    public record PassengerLine(int age, Passenger.Details details) {}

    public Quote quote(Departure departure, List<PassengerInput> inputs, List<SurchargeSelection> selections,
                       List<PassengerType> passengerTypes) {
        LocalDate departureDate = departure.getDepartureAt().toLocalDate();
        List<PassengerLine> lines = new ArrayList<>();
        for (PassengerInput p : inputs) {
            if (p.birthDate().isAfter(departureDate)) {
                throw ApiException.badRequest("INVALID_BIRTH_DATE", "Ngày sinh của " + p.fullName() + " không hợp lệ");
            }
            int age = Period.between(p.birthDate(), departureDate).getYears();
            PassengerType type = passengerTypes.stream().filter(t -> t.getAgeRange().contains(age)).findFirst()
                    .orElseThrow(() -> ApiException.badRequest("NO_PASSENGER_TYPE", "Không có loại khách cho " + p.fullName() + " (" + age + " tuổi)"));
            long price = departure.priceFor(type)
                    .orElseThrow(() -> ApiException.badRequest("MISSING_PRICE", "Đợt này chưa có giá cho loại khách " + type.getName()))
                    .getPrice();
            DeparturePickup pickup = p.pickupPointId() == null ? null : departure.pickupAt(p.pickupPointId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_PICKUP", "Điểm đón không thuộc đợt khởi hành này"));
            lines.add(new PassengerLine(age, new Passenger.Details(p.fullName(), p.birthDate(), p.gender(), blankToNull(p.phone()),
                    blankToNull(p.idNumber()), type, price,
                    pickup == null ? null : pickup.getPickupPoint(), pickup == null ? null : pickup.getPickupTime(),
                    pickup == null ? 0 : pickup.getFee())));
        }
        if (lines.stream().noneMatch(l -> l.age() >= ADULT_AGE)) {
            throw ApiException.badRequest("ADULT_REQUIRED", "Đơn phải có ít nhất một hành khách từ " + ADULT_AGE + " tuổi");
        }

        List<BookingSurcharge> surcharges = surcharges(departure, selections == null ? List.of() : selections, lines.size());
        long ticketTotal = lines.stream().mapToLong(l -> l.details().price()).sum();
        long pickupTotal = lines.stream().mapToLong(l -> l.details().pickupFee()).sum();
        long surchargeTotal = surcharges.stream().mapToLong(BookingSurcharge::getAmount).sum();
        long total = ticketTotal + pickupTotal + surchargeTotal;
        int seats = lines.stream().mapToInt(l -> l.details().passengerType().getSeats()).sum();
        return new Quote(lines, surcharges, ticketTotal, pickupTotal, surchargeTotal, total, seats, deposit(total));
    }

    /** Tiền cọc tối thiểu, làm tròn lên hàng nghìn, không vượt tổng tiền. */
    public long deposit(long total) {
        long rounded = (total * depositPercent + 99_999) / 100_000 * 1_000;
        return Math.min(rounded, total);
    }

    private static List<BookingSurcharge> surcharges(Departure departure, List<SurchargeSelection> selections, int passengers) {
        Set<Long> seen = new HashSet<>();
        List<BookingSurcharge> result = new ArrayList<>();
        for (SurchargeSelection s : selections) {
            if (!seen.add(s.surchargeId())) {
                throw ApiException.badRequest("DUPLICATE_SURCHARGE", "Mỗi phụ thu chỉ chọn một lần");
            }
            Surcharge surcharge = departure.getTour().getSurcharges().stream()
                    .filter(x -> x.getId().equals(s.surchargeId()) && x.isActive()).findFirst()
                    .orElseThrow(() -> ApiException.badRequest("SURCHARGE_NOT_AVAILABLE", "Phụ thu không áp dụng cho tour này"));
            result.add(new BookingSurcharge(surcharge, surcharge.getMethod().quantity(passengers, s.quantity())));
        }
        return result;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
