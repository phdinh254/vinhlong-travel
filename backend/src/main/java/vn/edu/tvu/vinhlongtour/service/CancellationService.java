package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.*;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Hủy toàn bộ/từng phần, hoàn tiền nhiều lần, chuyển đợt (Q35–Q39) */
@Service
@RequiredArgsConstructor
public class CancellationService {
    private final BookingService bookingService;
    private final BookingSupport support;
    private final PricingService pricing;
    private final DonDatTourRepository donRepo;
    private final DotKhoiHanhRepository dotRepo;
    private final YeuCauHuyRepository yeuCauRepo;
    private final HoanTienRepository hoanTienRepo;
    private final LichSuChuyenDotRepository chuyenDotRepo;

    /** Q37: % phạt theo chính sách đã chốt trên đơn và số ngày còn lại trước khởi hành */
    public BigDecimal phanTramPhat(DonDatTour don, LocalDate ngayHuy) {
        ChinhSachHuy cs = don.getChinhSachHuy();
        if (cs == null) return BigDecimal.ZERO;
        long soNgay = ChronoUnit.DAYS.between(ngayHuy, don.getDotKhoiHanh().getNgayGioDi().toLocalDate());
        return cs.getMocPhats().stream()
                .filter(m -> soNgay >= m.getSoNgayTruocTu() && soNgay <= m.getSoNgayTruocDen())
                .map(MocPhatHuy::getPhanTramPhat)
                .max(Comparator.naturalOrder())
                .orElse(soNgay < 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
    }

    private static BigDecimal phanTram(BigDecimal soTien, BigDecimal pt) {
        return soTien.multiply(pt).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    /** Q35-Q36: khách (hoặc khách vãng lai qua SĐT) gửi yêu cầu hủy */
    @Transactional
    public BookingDetail requestCancel(String maDon, CancelRequestInput r) {
        DonDatTour don = bookingService.loadWithAccess(maDon, r.sdt());
        if (!BookingSupport.TRANG_THAI_HOAT_DONG.contains(don.getTrangThai()))
            throw ApiException.conflict("Đơn ở trạng thái " + don.getTrangThai() + " không thể hủy");
        if (!don.getDotKhoiHanh().getNgayGioDi().isAfter(LocalDateTime.now()))
            throw ApiException.conflict("Tour đã khởi hành, không thể gửi yêu cầu hủy trực tuyến");
        if (yeuCauRepo.existsByDonDatTour_DonDatTourIdAndKetQua(don.getDonDatTourId(), YeuCauHuy.CHO_XU_LY))
            throw ApiException.conflict("Đơn đang có yêu cầu hủy chờ xử lý");

        List<HanhKhach> conHieuLuc = don.getHanhKhachs().stream().filter(h -> "HIEU_LUC".equals(h.getTrangThai())).toList();
        List<HanhKhach> huy;
        String loai = r.loaiHuy();
        if ("TUNG_PHAN".equals(loai)) {
            Set<Integer> ids = new HashSet<>(r.hanhKhachIds() == null ? List.of() : r.hanhKhachIds());
            huy = conHieuLuc.stream().filter(h -> ids.contains(h.getHanhKhachId())).toList();
            if (huy.isEmpty()) throw ApiException.badRequest("Chọn ít nhất một hành khách cần hủy");
            if (huy.size() == conHieuLuc.size()) loai = "TOAN_BO";
            else {
                List<HanhKhach> conLai = conHieuLuc.stream().filter(h -> !huy.contains(h)).toList();
                LocalDate ngayDi = don.getDotKhoiHanh().getNgayGioDi().toLocalDate();
                if (conLai.stream().noneMatch(h -> pricing.tinhTuoi(h.getNgaySinh(), ngayDi) >= 18))
                    throw ApiException.badRequest("Sau khi hủy, đơn phải còn ít nhất 1 hành khách từ 18 tuổi");
            }
        } else {
            huy = conHieuLuc;
        }

        TaiKhoan nguoi = support.currentAccount();
        BigDecimal pt = phanTramPhat(don, LocalDate.now());
        YeuCauHuy yc = new YeuCauHuy();
        yc.setDonDatTour(don);
        yc.setLoaiHuy(loai);
        yc.setLyDo(r.lyDo());
        yc.setNguoiYeuCau(nguoi);

        BigDecimal tongPhat = BigDecimal.ZERO;
        BigDecimal giaTriHuy = BigDecimal.ZERO;
        for (HanhKhach h : huy) {
            BigDecimal giaTri = h.getGiaApDung().add(h.getPhiDon());
            BigDecimal phat = phanTram(giaTri, pt);
            ChiTietHuyHanhKhach ct = new ChiTietHuyHanhKhach();
            ct.setYeuCauHuy(yc);
            ct.setHanhKhach(h);
            ct.setPhiPhat(phat);
            yc.getChiTiets().add(ct);
            tongPhat = tongPhat.add(phat);
            giaTriHuy = giaTriHuy.add(giaTri);
        }
        BigDecimal duKienHoan;
        if ("TOAN_BO".equals(loai)) {
            tongPhat = phanTram(don.getTongTien(), pt);              // phạt trên toàn bộ giá trị đơn
            duKienHoan = don.getDaThanhToan().subtract(tongPhat).max(BigDecimal.ZERO);
        } else {
            BigDecimal tongMoi = don.getTongTien().subtract(giaTriHuy).add(tongPhat);
            duKienHoan = don.getDaThanhToan().subtract(tongMoi).max(BigDecimal.ZERO);
        }
        yc.setTongPhiPhat(tongPhat);
        yc.setSoTienDuKienHoan(duKienHoan);
        yeuCauRepo.save(yc);

        // Chưa thu đồng nào và hủy toàn bộ: hủy ngay, không cần duyệt
        if ("TOAN_BO".equals(loai) && don.getDaThanhToan().signum() == 0) {
            apDungHuy(yc, null, "Tự động chấp nhận (đơn chưa thanh toán)");
        }
        return bookingService.toDetail(don);
    }

    /** Admin duyệt / từ chối yêu cầu hủy */
    @Transactional
    public BookingDetail process(Integer yeuCauId, ProcessCancelRequest r) {
        YeuCauHuy yc = yeuCauRepo.findById(yeuCauId).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu hủy"));
        if (!YeuCauHuy.CHO_XU_LY.equals(yc.getKetQua())) throw ApiException.conflict("Yêu cầu đã được xử lý");
        TaiKhoan admin = support.currentAccount();
        if (Boolean.TRUE.equals(r.chapNhan())) {
            apDungHuy(yc, admin, r.ghiChu());
        } else {
            yc.setKetQua(YeuCauHuy.TU_CHOI);
            yc.setNguoiXuLy(admin);
            yc.setThoiGianXuLy(LocalDateTime.now());
            yc.setGhiChuXuLy(r.ghiChu());
        }
        return bookingService.toDetail(yc.getDonDatTour());
    }

    private void apDungHuy(YeuCauHuy yc, TaiKhoan admin, String ghiChu) {
        DonDatTour don = yc.getDonDatTour();
        DotKhoiHanh dot = dotRepo.findByIdForUpdate(don.getDotKhoiHanh().getDotKhoiHanhId()).orElseThrow();
        if ("TOAN_BO".equals(yc.getLoaiHuy())) {
            support.traCho(dot, don.getSoChoChiem());
            don.getHanhKhachs().forEach(h -> h.setTrangThai("DA_HUY"));
            support.doiTrangThai(don, DonDatTour.DA_HUY, admin, "Hủy toàn bộ: " + Objects.toString(yc.getLyDo(), ""));
        } else {
            int choTra = 0;
            BigDecimal ve = BigDecimal.ZERO, phiDon = BigDecimal.ZERO;
            for (ChiTietHuyHanhKhach ct : yc.getChiTiets()) {
                HanhKhach h = ct.getHanhKhach();
                h.setTrangThai("DA_HUY");
                choTra += h.getLoaiKhach().getHeSoChiemCho();
                ve = ve.add(h.getGiaApDung());
                phiDon = phiDon.add(h.getPhiDon());
            }
            support.traCho(dot, choTra);
            don.setSoChoChiem(don.getSoChoChiem() - choTra);
            don.setTongTienVe(don.getTongTienVe().subtract(ve));
            // Phí phạt hủy được giữ lại, ghi vào tổng phụ thu của đơn
            don.setTongPhuThu(don.getTongPhuThu().subtract(phiDon).add(yc.getTongPhiPhat()));
            don.setTongTien(don.getTongTienVe().add(don.getTongPhuThu()));
            support.ghiLichSu(don, don.getTrangThai(), don.getTrangThai(), admin,
                    "Hủy " + yc.getChiTiets().size() + " hành khách, phí phạt " + yc.getTongPhiPhat());
            support.capNhatTrangThaiThanhToan(don, admin, "Cập nhật sau khi hủy từng phần");
        }
        yc.setKetQua(YeuCauHuy.CHAP_NHAN);
        yc.setNguoiXuLy(admin);
        yc.setThoiGianXuLy(LocalDateTime.now());
        yc.setGhiChuXuLy(ghiChu);
    }

    /** Q38: ghi nhận một lần hoàn tiền (có thể nhiều lần cho một đơn) */
    @Transactional
    public BookingDetail refund(Integer donId, RefundRequest r) {
        DonDatTour don = donRepo.findById(donId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (r.soTienHoan().compareTo(don.getDaThanhToan()) > 0)
            throw ApiException.badRequest("Số tiền hoàn vượt quá số tiền đã thu (" + don.getDaThanhToan() + ")");
        YeuCauHuy yc = null;
        if (r.yeuCauHuyId() != null) {
            yc = yeuCauRepo.findById(r.yeuCauHuyId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu hủy"));
            if (!yc.getDonDatTour().getDonDatTourId().equals(donId)) throw ApiException.badRequest("Yêu cầu hủy không thuộc đơn này");
        }
        TaiKhoan admin = support.currentAccount();
        HoanTien ht = new HoanTien();
        ht.setDonDatTour(don);
        ht.setYeuCauHuy(yc);
        ht.setSoTienHoan(r.soTienHoan());
        ht.setPhuongThuc(r.phuongThuc() == null ? "CHUYEN_KHOAN" : r.phuongThuc());
        ht.setMaGiaoDichHoan(r.maGiaoDichHoan());
        ht.setAnhChungTu(r.anhChungTu());
        ht.setGhiChu(r.ghiChu());
        ht.setNguoiThucHien(admin);
        hoanTienRepo.save(ht);
        // DaThanhToan lưu số tiền thực thu ròng (đã trừ hoàn)
        don.setDaThanhToan(don.getDaThanhToan().subtract(r.soTienHoan()));
        support.ghiLichSu(don, don.getTrangThai(), don.getTrangThai(), admin, "Hoàn tiền " + r.soTienHoan());
        support.capNhatTrangThaiThanhToan(don, admin, "Cập nhật sau hoàn tiền");
        return bookingService.toDetail(don);
    }

    /** Q39: chuyển đơn sang đợt khởi hành khác, tính lại giá theo bảng giá đợt mới */
    @Transactional
    public BookingDetail transfer(Integer donId, TransferRequest r) {
        DonDatTour don = donRepo.findById(donId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (!BookingSupport.TRANG_THAI_HOAT_DONG.contains(don.getTrangThai()))
            throw ApiException.conflict("Chỉ chuyển đợt được với đơn đang hiệu lực");
        Integer cuId = don.getDotKhoiHanh().getDotKhoiHanhId();
        if (cuId.equals(r.dotMoiId())) throw ApiException.badRequest("Đợt mới trùng đợt hiện tại");

        // Khóa theo thứ tự ID để tránh deadlock
        DotKhoiHanh a = dotRepo.findByIdForUpdate(Math.min(cuId, r.dotMoiId())).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
        DotKhoiHanh b = dotRepo.findByIdForUpdate(Math.max(cuId, r.dotMoiId())).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
        DotKhoiHanh cu = a.getDotKhoiHanhId().equals(cuId) ? a : b;
        DotKhoiHanh moi = cu == a ? b : a;
        if (!moi.getTour().getTourId().equals(cu.getTour().getTourId()))
            throw ApiException.badRequest("Chỉ chuyển sang đợt khác của cùng tour");
        if (!"MO_BAN".equals(moi.getTrangThai())) throw ApiException.conflict("Đợt mới không mở bán");

        Map<Integer, DotKhoiHanhDiemDon> diemDonMoi = new HashMap<>();
        moi.getDiemDons().forEach(x -> diemDonMoi.put(x.getDiemDon().getDiemDonId(), x));
        LocalDate ngayDi = moi.getNgayGioDi().toLocalDate();
        BigDecimal ve = BigDecimal.ZERO, phiDonCu = BigDecimal.ZERO, phiDonMoi = BigDecimal.ZERO;
        int cho = 0;
        for (HanhKhach h : don.getHanhKhachs()) {
            if (!"HIEU_LUC".equals(h.getTrangThai())) continue;
            BangGiaDot bg = pricing.timGia(moi, pricing.tinhTuoi(h.getNgaySinh(), ngayDi));
            if (bg == null) throw ApiException.badRequest("Đợt mới chưa có giá cho hành khách " + h.getHoTen());
            phiDonCu = phiDonCu.add(h.getPhiDon());
            h.setLoaiKhach(bg.getLoaiKhach());
            h.setGiaApDung(bg.getDonGia());
            DotKhoiHanhDiemDon dd = h.getDiemDon() == null ? null : diemDonMoi.get(h.getDiemDon().getDiemDonId());
            if (dd == null) h.setDiemDon(null);
            h.setPhiDon(dd == null ? BigDecimal.ZERO : dd.getPhiPhuThuDon());
            phiDonMoi = phiDonMoi.add(h.getPhiDon());
            ve = ve.add(h.getGiaApDung());
            cho += bg.getLoaiKhach().getHeSoChiemCho();
        }
        support.traCho(cu, don.getSoChoChiem());
        support.giuCho(moi, cho);

        BigDecimal tongCu = don.getTongTien();
        don.setDotKhoiHanh(moi);
        don.setSoChoChiem(cho);
        don.setTongTienVe(ve);
        don.setTongPhuThu(don.getTongPhuThu().subtract(phiDonCu).add(phiDonMoi));
        don.setTongTien(ve.add(don.getTongPhuThu()));

        TaiKhoan admin = support.currentAccount();
        LichSuChuyenDot ls = new LichSuChuyenDot();
        ls.setDonDatTour(don);
        ls.setDotCu(cu);
        ls.setDotMoi(moi);
        ls.setLyDo(r.lyDo());
        ls.setChenhLechTien(don.getTongTien().subtract(tongCu));
        ls.setNguoiThucHien(admin);
        chuyenDotRepo.save(ls);
        support.ghiLichSu(don, don.getTrangThai(), don.getTrangThai(), admin,
                "Chuyển từ đợt " + cu.getMaDot() + " sang " + moi.getMaDot() + ", chênh lệch " + ls.getChenhLechTien());
        support.capNhatTrangThaiThanhToan(don, admin, "Cập nhật sau chuyển đợt");
        return bookingService.toDetail(don);
    }

    @Transactional(readOnly = true)
    public List<CancelDto> listAll() {
        return yeuCauRepo.findAllByOrderByThoiGianYeuCauDesc().stream().map(CancellationService::toCancelDto).toList();
    }

    public static CancelDto toCancelDto(YeuCauHuy y) {
        return new CancelDto(y.getYeuCauHuyId(), y.getLoaiHuy(), y.getLyDo(), y.getThoiGianYeuCau(), y.getKetQua(),
                y.getTongPhiPhat(), y.getSoTienDuKienHoan(), y.getGhiChuXuLy(), y.getThoiGianXuLy(),
                y.getChiTiets().stream().map(c -> c.getHanhKhach().getHoTen()).toList(), y.getDonDatTour().getMaDon());
    }
}
