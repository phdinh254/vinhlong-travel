package vn.edu.tvu.vinhlongtour.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/vnpay/create")
    public PaymentUrlResponse create(@Valid @RequestBody CreatePaymentRequest r, HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        else ip = ip.split(",")[0].trim();
        return paymentService.createVnPay(r, ip);
    }

    /** Frontend gọi với nguyên query string mà VNPay trả về Return URL */
    @GetMapping("/vnpay/return")
    public VnPayResult vnpReturn(@RequestParam Map<String, String> params) {
        return paymentService.handleReturn(params);
    }

    /** Khai báo URL này (public HTTPS) làm IPN URL trên cổng VNPay */
    @GetMapping("/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        return paymentService.handleIpn(params);
    }
}
