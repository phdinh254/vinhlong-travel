package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;
import vn.edu.tvu.vinhlongtour.security.AuthUser;
import vn.edu.tvu.vinhlongtour.security.CurrentUser;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/** Nghiệp vụ đặt tour (Q22–Q31) */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DotKhoiHanhRepository dotRepo;
    private final DonDatTourRepository donRepo;
    private final KhachHangRepository khachHangRepo;
    private final ChinhSachHuyRepository chinhSachRepo;
    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final YeuCauHuyRepository yeuCauHuyRepo;
    private final HoanTienRepository hoanTienRepo;
    private final LichSuChuyenDotRepository chuyenDotRepo;
    private final PricingService pricing;
    private final BookingSupport support;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public Quote quote(CreateBookingRequest r) {
        DotKhoiHanh dot = dotRepo.findById(r.dotKhoiHanhId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt khởi hành"));
        PricingService.Calculation c = pricing.calculate(dot, r.hanhKhachs(), r.phuThus());
        return c.toQuote(pricing.tienCoc(c.tongTien()));
    }

    @Transactional
    public BookingDetail create(CreateBookingRequest r) {
        DotKhoiHanh dot = dotRepo.findByIdForUpdate(r.dotKhoiHanhId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt khởi hành"));
        LocalDateTime now = LocalDateTime.now();
        if (!"MO_BAN".equals(dot.getTrangThai()) || !"DANG_BAN".equals(dot.getTour().getTrangThai())) {
            throw ApiException.conflict("Đợt khởi hành không còn mở bán");
        }
        if (dot.getHanNhanKhach().isBefore(now)) throw ApiException.conflict("Đã hết hạn nhận khách cho đợt này");

        PricingService.Calculation c = pricing.calculate(dot, r.hanhKhachs(), r.phuThus());
        support.giuCho(dot, c.soChoChiem());

        DonDatTour don = new DonDatTour();
        don.setMaDon(taoMaDon());
        don.setDotKhoiHanh(dot);
        don.setKhachHang(timHoacTaoKhachHang(r));
        don.setTenNguoiDat(r.tenNguoiDat().trim());
        don.setSdtNguoiDat(r.sdtNguoiDat().trim());
        don.setEmailNguoiDat(r.emailNguoiDat());
        don.setGhiChu(r.ghiChu());
        don.setTongTienVe(c.tongTienVe());
        don.setTongPhuThu(c.tongPhuThu());
        don.setTongTien(c.tongTien());
        don.setSoChoChiem(c.soChoChiem());
        don.setNgayDat(now);
        don.setHanGiuCho(now.plusMinutes(props.booking().holdMinutes()));
        don.setChinhSachHuy(chinhSachRepo.findHieuLuc(dot.getTour().getTourId(), LocalDate.now()).stream().findFirst().orElse(null));

        for (PricingService.PassengerPrice p : c.passengers()) {
            HanhKhach hk = new HanhKhach();
            hk.setDonDatTour(don);
            hk.setHoTen(p.input().hoTen().trim());
            hk.setNgaySinh(p.input().ngaySinh());
            hk.setGioiTinh(p.input().gioiTinh());
            hk.setSoDienThoai(p.input().soDienThoai());
            hk.setCccdPassport(p.input().cccdPassport());
            hk.setLoaiKhach(p.loaiKhach());
            hk.setDiemDon(p.diemDon());
            hk.setGiaApDung(p.gia());      // Q28
            hk.setPhiDon(p.phiDon());
            don.getHanhKhachs().add(hk);
        }
        for (SurchargeLine l : c.lines()) {
            ChiTietPhuThuDon ct = new ChiTietPhuThuDon();
            ct.setDonDatTour(don);
            ct.setPhuThu(c.phuThuMap().get(l.phuThuId()));
            ct.setSoLuong(l.soLuong());
            ct.setDonGia(l.donGia());
            ct.setThanhTien(l.thanhTien());
            don.getPhuThus().add(ct);
        }
        if (c.tongTien().signum() == 0) don.setTrangThai(DonDatTour.DA_THANH_TOAN);
        donRepo.save(don);
        support.ghiLichSu(don, null, don.getTrangThai(), support.currentAccount(), "Tạo đơn đặt tour");
        return toDetail(don);
    }

    /** Khách xem đơn: chủ tài khoản, admin, hoặc khách vãng lai cung cấp đúng SĐT người đặt */
    @Transactional(readOnly = true)
    public BookingDetail getForCustomer(String maDon, String sdt) {
        return toDetail(loadWithAccess(maDon, sdt));
    }

    public DonDatTour loadWithAccess(String maDon, String sdt) {
        DonDatTour don = donRepo.findByMaDon(maDon).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        Optional<AuthUser> u = CurrentUser.get();
        if (u.isPresent() && u.get().isAdmin()) return don;
        TaiKhoan chu = don.getKhachHang().getTaiKhoan();
        if (u.isPresent() && chu != null && chu.getTaiKhoanId().equals(u.get().taiKhoanId())) return don;
        if (sdt != null && sdt.trim().equals(don.getSdtNguoiDat())) return don;
        throw ApiException.notFound("Không tìm thấy đơn hoặc số điện thoại không khớp");
    }

    @Transactional(readOnly = true)
    public List<BookingSummary> myBookings(Integer taiKhoanId) {
        return khachHangRepo.findByTaiKhoan_TaiKhoanId(taiKhoanId)
                .map(kh -> donRepo.findByKhachHang_KhachHangIdOrderByNgayDatDesc(kh.getKhachHangId()).stream()
                        .map(this::toSummary).toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Page<BookingSummary> adminSearch(String q, String trangThai, Integer tourId, Integer dotId, int page, int size) {
        Specification<DonDatTour> spec = (root, query, cb) -> {
            var p = cb.conjunction();
            if (trangThai != null && !trangThai.isBlank()) p = cb.and(p, cb.equal(root.get("trangThai"), trangThai));
            if (dotId != null) p = cb.and(p, cb.equal(root.get("dotKhoiHanh").get("dotKhoiHanhId"), dotId));
            if (tourId != null) p = cb.and(p, cb.equal(root.get("dotKhoiHanh").get("tour").get("tourId"), tourId));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p = cb.and(p, cb.or(cb.like(cb.lower(root.get("maDon")), like),
                        cb.like(cb.lower(root.get("tenNguoiDat")), like),
                        cb.like(root.get("sdtNguoiDat"), like)));
            }
            return p;
        };
        return donRepo.findAll(spec, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "ngayDat")))
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public BookingDetail adminDetail(Integer id) {
        return toDetail(donRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn")));
    }

    /** Admin đổi trạng thái: HOAN_THANH (sau chuyến đi) hoặc DA_HUY (hủy trực tiếp, trả chỗ) */
    @Transactional
    public BookingDetail adminUpdateStatus(Integer id, String trangThai, String lyDo) {
        DonDatTour don = donRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (!BookingSupport.TRANG_THAI_HOAT_DONG.contains(don.getTrangThai())) {
            throw ApiException.conflict("Đơn ở trạng thái " + don.getTrangThai() + " không thể thay đổi");
        }
        TaiKhoan admin = support.currentAccount();
        if (DonDatTour.DA_HUY.equals(trangThai)) {
            DotKhoiHanh dot = dotRepo.findByIdForUpdate(don.getDotKhoiHanh().getDotKhoiHanhId()).orElseThrow();
            support.traCho(dot, don.getSoChoChiem());
            don.getHanhKhachs().forEach(h -> h.setTrangThai("DA_HUY"));
        } else if (DonDatTour.HOAN_THANH.equals(trangThai) && !DonDatTour.DA_THANH_TOAN.equals(don.getTrangThai())) {
            throw ApiException.conflict("Chỉ hoàn thành được đơn đã thanh toán đủ");
        }
        support.doiTrangThai(don, trangThai, admin, lyDo);
        return toDetail(don);
    }

    /** Q31: hết hạn giữ chỗ — được BookingExpiryJob gọi cho từng đơn */
    @Transactional
    public void expireIfNeeded(Integer donId) {
        DonDatTour don = donRepo.findById(donId).orElse(null);
        if (don == null) return;
        DotKhoiHanh dot = dotRepo.findByIdForUpdate(don.getDotKhoiHanh().getDotKhoiHanhId()).orElseThrow();
        if (!DonDatTour.CHO_THANH_TOAN.equals(don.getTrangThai()) || don.getDaThanhToan().signum() > 0
                || don.getHanGiuCho() == null || don.getHanGiuCho().isAfter(LocalDateTime.now())) return;
        support.traCho(dot, don.getSoChoChiem());
        support.doiTrangThai(don, DonDatTour.HET_HAN, null, "Quá hạn giữ chỗ chưa thanh toán");
        log.info("Đơn {} hết hạn giữ chỗ, trả {} chỗ cho đợt {}", don.getMaDon(), don.getSoChoChiem(), dot.getMaDot());
    }

    private KhachHang timHoacTaoKhachHang(CreateBookingRequest r) {
        Optional<AuthUser> u = CurrentUser.get();
        if (u.isPresent()) {
            Optional<KhachHang> kh = khachHangRepo.findByTaiKhoan_TaiKhoanId(u.get().taiKhoanId());
            if (kh.isPresent()) {
                if (kh.get().getSoDienThoai() == null) kh.get().setSoDienThoai(r.sdtNguoiDat());
                return kh.get();
            }
        }
        // Q23: khách vãng lai — dùng lại hồ sơ theo SĐT để lưu lịch sử giao dịch
        return khachHangRepo.findFirstBySoDienThoaiAndTaiKhoanIsNull(r.sdtNguoiDat().trim()).orElseGet(() -> {
            KhachHang kh = new KhachHang();
            kh.setHoTen(r.tenNguoiDat().trim());
            kh.setSoDienThoai(r.sdtNguoiDat().trim());
            kh.setEmail(r.emailNguoiDat());
            return khachHangRepo.save(kh);
        });
    }

    private String taoMaDon() {
        String prefix = "VL" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int j = 0; j < 5; j++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            if (donRepo.findByMaDon(sb.toString()).isEmpty()) return sb.toString();
        }
        throw new IllegalStateException("Không sinh được mã đơn");
    }

    public BookingSummary toSummary(DonDatTour d) {
        return new BookingSummary(d.getDonDatTourId(), d.getMaDon(), d.getDotKhoiHanh().getTour().getTenTour(),
                d.getDotKhoiHanh().getMaDot(), d.getDotKhoiHanh().getNgayGioDi(), d.getTenNguoiDat(), d.getSdtNguoiDat(),
                (int) d.getHanhKhachs().stream().filter(h -> "HIEU_LUC".equals(h.getTrangThai())).count(),
                d.getTongTien(), d.getDaThanhToan(), d.getTrangThai(), d.getNgayDat());
    }

    public BookingDetail toDetail(DonDatTour d) {
        Integer id = d.getDonDatTourId();
        DotKhoiHanh dot = d.getDotKhoiHanh();
        return new BookingDetail(id, d.getMaDon(), d.getTrangThai(),
                dot.getTour().getTourId(), dot.getTour().getMaTour(), dot.getTour().getTenTour(),
                dot.getDotKhoiHanhId(), dot.getMaDot(), dot.getNgayGioDi(), dot.getNgayGioVe(),
                d.getTenNguoiDat(), d.getSdtNguoiDat(), d.getEmailNguoiDat(), d.getGhiChu(),
                d.getTongTienVe(), d.getTongPhuThu(), d.getTongTien(), d.getDaThanhToan(), d.conLai(),
                d.getDaThanhToan().signum() == 0 ? pricing.tienCoc(d.getTongTien()) : java.math.BigDecimal.ZERO,
                d.getSoChoChiem(), d.getNgayDat(), d.getHanGiuCho(), d.getKhachHang().getTaiKhoan() == null,
                d.getHanhKhachs().stream().map(h -> new PassengerDto(h.getHanhKhachId(), h.getHoTen(), h.getNgaySinh(),
                        h.getGioiTinh(), h.getSoDienThoai(), h.getCccdPassport(), h.getLoaiKhach().getTenLoaiKhach(),
                        h.getGiaApDung(), h.getPhiDon(), h.getDiemDon() == null ? null : h.getDiemDon().getDiemDonId(),
                        h.getDiemDon() == null ? null : h.getDiemDon().getTenDiemDon(), h.getTrangThai())).toList(),
                d.getPhuThus().stream().map(p -> new SurchargeLine(p.getPhuThu().getPhuThuId(), p.getPhuThu().getTenPhuThu(),
                        p.getPhuThu().getCachTinh(), p.getSoLuong(), p.getDonGia(), p.getThanhTien())).toList(),
                id == null ? List.of() : thanhToanRepo.findByDonDatTour_DonDatTourIdOrderByNgayTaoAsc(id).stream()
                        .map(t -> new PaymentDto(t.getThanhToanId(), t.getSoTien(), t.getLoaiThanhToan(), t.getPhuongThuc(),
                                t.getMaGiaoDich(), t.getMaNganHang(), t.getThoiGianTT(), t.getTrangThai(), t.getAnhChungTu(),
                                t.getNguoiXacNhan() == null ? null : t.getNguoiXacNhan().getHoTen(), t.getNgayTao())).toList(),
                id == null ? List.of() : lichSuRepo.findByDonDatTour_DonDatTourIdOrderByThoiGianAsc(id).stream()
                        .map(l -> new HistoryDto(l.getTrangThaiCu(), l.getTrangThaiMoi(), l.getThoiGian(), l.getLyDo(),
                                l.getNguoiThayDoi() == null ? "Hệ thống" : l.getNguoiThayDoi().getHoTen())).toList(),
                id == null ? List.of() : yeuCauHuyRepo.findByDonDatTour_DonDatTourIdOrderByThoiGianYeuCauDesc(id).stream()
                        .map(CancellationService::toCancelDto).toList(),
                id == null ? List.of() : hoanTienRepo.findByDonDatTour_DonDatTourIdOrderByNgayHoanAsc(id).stream()
                        .map(h -> new RefundDto(h.getHoanTienId(), h.getYeuCauHuy() == null ? null : h.getYeuCauHuy().getYeuCauHuyId(),
                                h.getSoTienHoan(), h.getNgayHoan(), h.getPhuongThuc(), h.getMaGiaoDichHoan(),
                                h.getAnhChungTu(), h.getGhiChu())).toList(),
                id == null ? List.of() : chuyenDotRepo.findByDonDatTour_DonDatTourIdOrderByThoiGianAsc(id).stream()
                        .map(c -> new TransferDto(c.getDotCu().getMaDot(), c.getDotMoi().getMaDot(), c.getChenhLechTien(),
                                c.getLyDo(), c.getThoiGian())).toList(),
                Mapper.toPolicy(d.getChinhSachHuy()));
    }
}
