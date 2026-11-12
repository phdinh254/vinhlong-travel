package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.LichSuTrangThaiDonRepository;
import vn.edu.tvu.vinhlongtour.repository.TaiKhoanRepository;
import vn.edu.tvu.vinhlongtour.security.AuthUser;
import vn.edu.tvu.vinhlongtour.security.CurrentUser;

import java.math.BigDecimal;
import java.util.Set;

/** Các thao tác dùng chung: ghi lịch sử trạng thái, giữ/trả chỗ, tính lại trạng thái thanh toán */
@Component
@RequiredArgsConstructor
public class BookingSupport {
    public static final Set<String> TRANG_THAI_HOAT_DONG =
            Set.of(DonDatTour.CHO_THANH_TOAN, DonDatTour.DA_COC, DonDatTour.DA_THANH_TOAN);

    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final TaiKhoanRepository taiKhoanRepo;

    /** Q30: đổi trạng thái kèm ghi lịch sử. nguoi = null nghĩa là hệ thống tự động */
    public void doiTrangThai(DonDatTour don, String moi, TaiKhoan nguoi, String lyDo) {
        String cu = don.getTrangThai();
        if (moi.equals(cu)) return;
        don.setTrangThai(moi);
        ghiLichSu(don, cu, moi, nguoi, lyDo);
    }

    public void ghiLichSu(DonDatTour don, String cu, String moi, TaiKhoan nguoi, String lyDo) {
        LichSuTrangThaiDon ls = new LichSuTrangThaiDon();
        ls.setDonDatTour(don);
        ls.setTrangThaiCu(cu);
        ls.setTrangThaiMoi(moi);
        ls.setNguoiThayDoi(nguoi);
        ls.setLyDo(lyDo);
        lichSuRepo.save(ls);
    }

    /** Q15/Q19: giữ chỗ — gọi khi đã khóa dòng DotKhoiHanh */
    public void giuCho(DotKhoiHanh dot, int soCho) {
        if (dot.soChoConLai() < soCho) {
            throw ApiException.conflict("Đợt khởi hành chỉ còn " + dot.soChoConLai() + " chỗ");
        }
        dot.setSoChoDaDat(dot.getSoChoDaDat() + soCho);
        if (dot.soChoConLai() == 0 && "MO_BAN".equals(dot.getTrangThai())) dot.setTrangThai("HET_CHO");
    }

    /** Q31: trả chỗ — gọi khi đã khóa dòng DotKhoiHanh */
    public void traCho(DotKhoiHanh dot, int soCho) {
        dot.setSoChoDaDat(Math.max(0, dot.getSoChoDaDat() - soCho));
        if ("HET_CHO".equals(dot.getTrangThai()) && dot.soChoConLai() > 0) dot.setTrangThai("MO_BAN");
    }

    /** Tính lại trạng thái theo số tiền đã thu */
    public void capNhatTrangThaiThanhToan(DonDatTour don, TaiKhoan nguoi, String lyDo) {
        if (!TRANG_THAI_HOAT_DONG.contains(don.getTrangThai())) return;
        String moi;
        BigDecimal da = don.getDaThanhToan();
        if (don.getTongTien().signum() > 0 && da.compareTo(don.getTongTien()) >= 0) moi = DonDatTour.DA_THANH_TOAN;
        else if (da.signum() > 0) moi = DonDatTour.DA_COC;
        else moi = DonDatTour.CHO_THANH_TOAN;
        doiTrangThai(don, moi, nguoi, lyDo);
    }

    public TaiKhoan currentAccount() {
        return CurrentUser.get().map(AuthUser::taiKhoanId).flatMap(taiKhoanRepo::findById).orElse(null);
    }
}
