package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.PhuThuRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tính giá đơn đặt:
 *  - Q18: tuổi = số năm tròn tính đến NGÀY KHỞI HÀNH -> chọn LoaiKhach theo [TuoiTu, TuoiDen]
 *  - Q17: đơn giá theo BangGiaDot của đợt
 *  - Q19: số chỗ chiếm = tổng HeSoChiemCho
 *  - Q20: phụ thu điểm đón theo từng hành khách
 *  - Q21: phụ thu THEO_NGUOI / THEO_DON / THEO_SO_LUONG
 */
@Service
@RequiredArgsConstructor
public class PricingService {
    private final PhuThuRepository phuThuRepo;
    private final AppProperties props;

    public record PassengerPrice(PassengerInput input, LoaiKhach loaiKhach, BigDecimal gia, DiemDon diemDon, BigDecimal phiDon, int tuoi) {}

    public record Calculation(List<PassengerPrice> passengers, List<SurchargeLine> lines, Map<Integer, PhuThu> phuThuMap,
                              BigDecimal tongTienVe, BigDecimal tongPhiDon, BigDecimal tongPhuThu,
                              BigDecimal tongTien, int soChoChiem) {
        public Quote toQuote(BigDecimal coc) {
            return new Quote(passengers.stream().map(p -> new PassengerQuote(p.input().hoTen(), p.tuoi(),
                            p.loaiKhach().getLoaiKhachId(), p.loaiKhach().getTenLoaiKhach(), p.gia(), p.phiDon(),
                            p.loaiKhach().getHeSoChiemCho())).toList(),
                    lines, tongTienVe, tongPhiDon, tongPhuThu, tongTien, soChoChiem, coc);
        }
    }

    public int tinhTuoi(LocalDate ngaySinh, LocalDate ngayKhoiHanh) {
        return Period.between(ngaySinh, ngayKhoiHanh).getYears();
    }

    /** Giá của 1 hành khách theo bảng giá của đợt */
    public BangGiaDot timGia(DotKhoiHanh dot, int tuoi) {
        return dot.getBangGias().stream()
                .filter(b -> tuoi >= b.getLoaiKhach().getTuoiTu() && tuoi <= b.getLoaiKhach().getTuoiDen())
                .findFirst().orElse(null);
    }

    public Calculation calculate(DotKhoiHanh dot, List<PassengerInput> hanhKhachs, List<SurchargeInput> phuThus) {
        LocalDate ngayDi = dot.getNgayGioDi().toLocalDate();
        Map<Integer, DotKhoiHanhDiemDon> diemDonMap = dot.getDiemDons().stream()
                .collect(Collectors.toMap(x -> x.getDiemDon().getDiemDonId(), Function.identity()));

        List<PassengerPrice> list = new ArrayList<>();
        boolean coNguoiLon = false;
        for (PassengerInput p : hanhKhachs) {
            if (p.ngaySinh().isAfter(LocalDate.now())) throw ApiException.badRequest("Ngày sinh của " + p.hoTen() + " không hợp lệ");
            int tuoi = tinhTuoi(p.ngaySinh(), ngayDi);
            if (tuoi >= 18) coNguoiLon = true;
            BangGiaDot bg = timGia(dot, tuoi);
            if (bg == null) throw ApiException.badRequest("Đợt này chưa có giá cho hành khách " + p.hoTen() + " (" + tuoi + " tuổi)");
            DiemDon dd = null;
            BigDecimal phiDon = BigDecimal.ZERO;
            if (p.diemDonId() != null) {
                DotKhoiHanhDiemDon x = diemDonMap.get(p.diemDonId());
                if (x == null) throw ApiException.badRequest("Điểm đón không thuộc đợt khởi hành này");
                dd = x.getDiemDon();
                phiDon = x.getPhiPhuThuDon();
            }
            list.add(new PassengerPrice(p, bg.getLoaiKhach(), bg.getDonGia(), dd, phiDon, tuoi));
        }
        if (!coNguoiLon) throw ApiException.badRequest("Đơn đặt phải có ít nhất 1 hành khách từ 18 tuổi trở lên");

        Map<Integer, PhuThu> apDung = phuThuRepo.findApDungChoTour(dot.getTour().getTourId()).stream()
                .collect(Collectors.toMap(PhuThu::getPhuThuId, Function.identity()));
        List<SurchargeLine> lines = new ArrayList<>();
        if (phuThus != null) {
            for (SurchargeInput s : phuThus) {
                PhuThu pt = apDung.get(s.phuThuId());
                if (pt == null) throw ApiException.badRequest("Phụ thu không áp dụng cho tour này");
                if (lines.stream().anyMatch(l -> l.phuThuId().equals(pt.getPhuThuId()))) continue;
                int sl = switch (pt.getCachTinh()) {
                    case PhuThu.THEO_NGUOI -> list.size();
                    case PhuThu.THEO_DON -> 1;
                    default -> s.soLuong() == null ? 1 : s.soLuong();
                };
                lines.add(new SurchargeLine(pt.getPhuThuId(), pt.getTenPhuThu(), pt.getCachTinh(), sl,
                        pt.getDonGia(), pt.getDonGia().multiply(BigDecimal.valueOf(sl))));
            }
        }

        BigDecimal tongVe = list.stream().map(PassengerPrice::gia).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongPhiDon = list.stream().map(PassengerPrice::phiDon).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongPhuThu = lines.stream().map(SurchargeLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add).add(tongPhiDon);
        int cho = list.stream().mapToInt(p -> p.loaiKhach().getHeSoChiemCho()).sum();
        return new Calculation(list, lines, apDung, tongVe, tongPhiDon, tongPhuThu, tongVe.add(tongPhuThu), cho);
    }

    /** Q32: số tiền cọc tối thiểu, làm tròn lên hàng nghìn */
    public BigDecimal tienCoc(BigDecimal tongTien) {
        BigDecimal coc = tongTien.multiply(BigDecimal.valueOf(props.booking().depositPercent()))
                .divide(BigDecimal.valueOf(100_000), 0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(1000));
        return coc.min(tongTien);
    }
}
