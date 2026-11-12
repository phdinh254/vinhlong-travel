package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.ManualPaymentRequest;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.DonDatTourRepository;
import vn.edu.tvu.vinhlongtour.repository.DotKhoiHanhRepository;
import vn.edu.tvu.vinhlongtour.repository.ThanhToanRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thanh toán (Q32–Q34): VNPay + xác nhận thủ công */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final BookingService bookingService;
    private final BookingSupport support;
    private final PricingService pricing;
    private final VnPayService vnPay;
    private final ThanhToanRepository thanhToanRepo;
    private final DonDatTourRepository donRepo;
    private final DotKhoiHanhRepository dotRepo;
    private final AppProperties props;

    /** loai = COC (đặt cọc tối thiểu) | CON_LAI (thanh toán toàn bộ số còn lại) */
    @Transactional
    public PaymentUrlResponse createVnPay(CreatePaymentRequest r, String ipAddr) {
        DonDatTour don = bookingService.loadWithAccess(r.maDon(), r.sdt());
        if (!DonDatTour.CHO_THANH_TOAN.equals(don.getTrangThai()) && !DonDatTour.DA_COC.equals(don.getTrangThai())) {
            throw ApiException.conflict("Đơn không ở trạng thái chờ thanh toán");
        }
        if (DonDatTour.CHO_THANH_TOAN.equals(don.getTrangThai()) && don.getHanGiuCho() != null
                && don.getHanGiuCho().isBefore(LocalDateTime.now())) {
            throw ApiException.conflict("Đơn đã quá hạn giữ chỗ, vui lòng đặt lại");
        }
        BigDecimal soTien;
        String loai;
        if ("COC".equals(r.loai())) {
            if (don.getDaThanhToan().signum() > 0) throw ApiException.badRequest("Đơn đã đặt cọc, hãy thanh toán phần còn lại");
            soTien = pricing.tienCoc(don.getTongTien());
            loai = soTien.compareTo(don.getTongTien()) >= 0 ? "TOAN_BO" : "COC";
        } else {
            soTien = don.conLai();
            loai = don.getDaThanhToan().signum() == 0 ? "TOAN_BO" : "TAT_TOAN";
        }
        if (soTien.signum() <= 0) throw ApiException.badRequest("Không còn số tiền cần thanh toán");

        String txnRef = don.getMaDon() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        ThanhToan tt = new ThanhToan();
        tt.setDonDatTour(don);
        tt.setSoTien(soTien);
        tt.setLoaiThanhToan(loai);
        tt.setPhuongThuc("VNPAY");
        tt.setMaThamChieu(txnRef);
        thanhToanRepo.save(tt);

        String url = vnPay.createPaymentUrl(txnRef, soTien, "Thanh toan don dat tour " + don.getMaDon(), ipAddr);
        return new PaymentUrlResponse(url, soTien);
    }

    /** Return URL: frontend chuyển nguyên query string VNPay về đây để kiểm tra và hiển thị */
    @Transactional
    public VnPayResult handleReturn(Map<String, String> params) {
        if (!vnPay.verify(params)) {
            return new VnPayResult(false, false, null, null, null, params.get("vnp_ResponseCode"), "Chữ ký không hợp lệ");
        }
        if (props.vnpay().processOnReturn()) {
            process(params);
        }
        ThanhToan tt = thanhToanRepo.findByMaThamChieuForUpdate(params.get("vnp_TxnRef")).orElse(null);
        boolean ok = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        return new VnPayResult(true, ok, tt == null ? null : tt.getDonDatTour().getMaDon(),
                tt == null ? null : tt.getSoTien(), params.get("vnp_TransactionNo"), params.get("vnp_ResponseCode"),
                ok ? "Thanh toán thành công" : "Giao dịch không thành công (mã " + params.get("vnp_ResponseCode") + ")");
    }

    /** IPN: VNPay gọi server-to-server. Trả về RspCode theo tài liệu VNPay */
    @Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        try {
            if (!vnPay.verify(params)) return rsp("97", "Invalid Checksum");
            return process(params);
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN VNPay", e);
            return rsp("99", "Unknown error");
        }
    }

    /** Xử lý idempotent: một giao dịch chỉ được ghi nhận thành công 1 lần */
    private Map<String, String> process(Map<String, String> params) {
        ThanhToan tt = thanhToanRepo.findByMaThamChieuForUpdate(params.get("vnp_TxnRef")).orElse(null);
        if (tt == null) return rsp("01", "Order not found");
        BigDecimal amount = new BigDecimal(params.getOrDefault("vnp_Amount", "0")).divide(BigDecimal.valueOf(100));
        if (amount.compareTo(tt.getSoTien()) != 0) return rsp("04", "Invalid amount");
        if (!ThanhToan.CHO_THANH_TOAN.equals(tt.getTrangThai())) return rsp("02", "Order already confirmed");

        tt.setDuLieuPhanHoi(new LinkedHashMap<>(params).toString());
        tt.setMaGiaoDich(params.get("vnp_TransactionNo"));
        tt.setMaNganHang(params.get("vnp_BankCode"));
        boolean ok = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        if (!ok) {
            tt.setTrangThai(ThanhToan.THAT_BAI);
            return rsp("00", "Confirm Success");
        }
        try {
            tt.setThoiGianTT(LocalDateTime.parse(params.get("vnp_PayDate"), VnPayService.FMT));
        } catch (Exception e) {
            tt.setThoiGianTT(LocalDateTime.now());
        }
        ghiNhanThanhCong(tt, null, "Thanh toán VNPay " + tt.getLoaiThanhToan() + " - GD " + tt.getMaGiaoDich());
        return rsp("00", "Confirm Success");
    }

    /** Admin xác nhận thanh toán chuyển khoản / tiền mặt kèm chứng từ (Q33-Q34) */
    @Transactional
    public BookingDetail confirmManual(Integer donId, ManualPaymentRequest r) {
        DonDatTour don = donRepo.findById(donId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (!BookingSupport.TRANG_THAI_HOAT_DONG.contains(don.getTrangThai()) && !DonDatTour.HET_HAN.equals(don.getTrangThai())) {
            throw ApiException.conflict("Đơn ở trạng thái " + don.getTrangThai() + " không nhận thanh toán");
        }
        if (r.soTien().compareTo(don.conLai()) > 0) throw ApiException.badRequest("Số tiền vượt quá số còn phải thu (" + don.conLai() + ")");
        TaiKhoan admin = support.currentAccount();
        ThanhToan tt = new ThanhToan();
        tt.setDonDatTour(don);
        tt.setSoTien(r.soTien());
        tt.setLoaiThanhToan(r.loaiThanhToan());
        tt.setPhuongThuc(r.phuongThuc());
        tt.setMaGiaoDich(r.maGiaoDich());
        tt.setAnhChungTu(r.anhChungTu());
        tt.setThoiGianTT(LocalDateTime.now());
        tt.setNguoiXacNhan(admin);
        thanhToanRepo.save(tt);
        ghiNhanThanhCong(tt, admin, "Xác nhận thanh toán " + r.phuongThuc());
        return bookingService.toDetail(don);
    }

    private void ghiNhanThanhCong(ThanhToan tt, TaiKhoan nguoi, String lyDo) {
        tt.setTrangThai(ThanhToan.THANH_CONG);
        DonDatTour don = tt.getDonDatTour();
        // Thanh toán về sau khi đơn đã hết hạn: cố gắng giữ lại chỗ
        if (DonDatTour.HET_HAN.equals(don.getTrangThai())) {
            DotKhoiHanh dot = dotRepo.findByIdForUpdate(don.getDotKhoiHanh().getDotKhoiHanhId()).orElseThrow();
            if (dot.soChoConLai() >= don.getSoChoChiem()) {
                support.giuCho(dot, don.getSoChoChiem());
                support.doiTrangThai(don, DonDatTour.CHO_THANH_TOAN, nguoi, "Khôi phục đơn do nhận thanh toán sau hạn giữ chỗ");
            } else {
                don.setDaThanhToan(don.getDaThanhToan().add(tt.getSoTien()));
                support.ghiLichSu(don, don.getTrangThai(), don.getTrangThai(), nguoi,
                        "Nhận " + tt.getSoTien() + "đ sau khi đơn hết hạn và đợt đã hết chỗ - CẦN HOÀN TIỀN");
                return;
            }
        }
        don.setDaThanhToan(don.getDaThanhToan().add(tt.getSoTien()));
        support.capNhatTrangThaiThanhToan(don, nguoi, lyDo);
    }

    private static Map<String, String> rsp(String code, String msg) {
        return Map.of("RspCode", code, "Message", msg);
    }
}
