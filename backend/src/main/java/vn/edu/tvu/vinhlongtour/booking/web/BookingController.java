package vn.edu.tvu.vinhlongtour.booking.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.auth.security.CurrentAccount;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingDetail;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingSummary;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.QuoteRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.QuoteResponse;

import java.util.List;

/**
 * API đặt tour cho khách: báo giá, đặt tour (khách vãng lai hoặc đã đăng nhập), tra cứu đơn bằng mã + SĐT,
 * và "đơn của tôi" cho khách đã đăng nhập.
 */
@RestController
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/bookings/quote")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest r) {
        return service.quote(r);
    }

    /** Đã đăng nhập thì đơn gắn với tài khoản (xem được ở "đơn của tôi"); chưa đăng nhập vẫn đặt được. */
    @PostMapping("/api/v1/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail create(@Valid @RequestBody BookingRequest r, Authentication auth) {
        return service.create(r, CurrentAccount.idOrNull(auth));
    }

    @GetMapping("/api/v1/bookings/lookup")
    public BookingDetail lookup(@RequestParam String code, @RequestParam String phone) {
        return service.lookup(code, phone);
    }

    @GetMapping("/api/v1/me/bookings")
    public List<BookingSummary> mine(Authentication auth) {
        return service.listMine(Long.valueOf(auth.getName()));
    }

    @GetMapping("/api/v1/me/bookings/{code}")
    public BookingDetail mine(Authentication auth, @PathVariable String code) {
        return service.getMine(Long.valueOf(auth.getName()), code);
    }
}
