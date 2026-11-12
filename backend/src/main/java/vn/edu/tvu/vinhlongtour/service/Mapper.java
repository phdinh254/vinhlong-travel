package vn.edu.tvu.vinhlongtour.service;

import vn.edu.tvu.vinhlongtour.dto.AdminDtos.NewsDto;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Chuyển entity -> DTO (gọi trong transaction để nạp quan hệ LAZY) */
public final class Mapper {
    private Mapper() {}

    public static String anhDaiDien(Tour t) {
        return t.getHinhAnhs().stream()
                .sorted(Comparator.comparing((HinhAnhTour h) -> !Boolean.TRUE.equals(h.getLaAnhDaiDien()))
                        .thenComparing(HinhAnhTour::getThuTuHienThi))
                .map(HinhAnhTour::getDuongDan).findFirst().orElse(null);
    }

    public static TourCard toCard(Tour t, BigDecimal giaTu) {
        String moTa = t.getMoTa() == null ? null : t.getMoTa().replaceAll("<[^>]*>", "");
        if (moTa != null && moTa.length() > 180) moTa = moTa.substring(0, 180) + "…";
        return new TourCard(t.getTourId(), t.getMaTour(), t.getTenTour(), t.getThoiLuong(), anhDaiDien(t), giaTu,
                t.getLoaiHinhs().stream().map(LoaiHinh::getTenLoaiHinh).sorted().toList(),
                t.getKhuVucs().stream().map(KhuVuc::getTenKhuVuc).sorted().toList(),
                t.getNoiBat(), t.getTrangThai(), moTa);
    }

    public static List<NgayDto> toNgays(List<NgayLichTrinh> ngays) {
        return ngays.stream()
                .sorted(Comparator.comparing(NgayLichTrinh::getNgayThuMay))
                .map(n -> new NgayDto(n.getNgayLichTrinhId(), n.getNgayThuMay(), n.getTieuDeNgay(),
                        n.getHoatDongs().stream()
                                .sorted(Comparator.comparing(HoatDongLichTrinh::getThuTuHienThi))
                                .map(h -> new HoatDongDto(h.getHoatDongId(), h.getLoaiHoatDong(), h.getGioBatDau(),
                                        h.getGioKetThuc(), h.getNoiDung(), h.getThuTuHienThi(),
                                        h.getDiemDen() == null ? null : h.getDiemDen().getDiemDenId(),
                                        h.getDiemDen() == null ? null : h.getDiemDen().getTenDiemDen()))
                                .toList()))
                .toList();
    }

    public static DotDto toDot(DotKhoiHanh d) {
        return new DotDto(d.getDotKhoiHanhId(), d.getTour().getTourId(), d.getMaDot(), d.getNgayGioDi(), d.getNgayGioVe(),
                d.getSucChuaToiDa(), d.getSoChoDaDat(), d.soChoConLai(), d.getHanNhanKhach(), d.getTrangThai(),
                d.getBangGias().stream()
                        .sorted(Comparator.comparing((BangGiaDot b) -> b.getLoaiKhach().getTuoiTu()).reversed())
                        .map(b -> new GiaDto(b.getLoaiKhach().getLoaiKhachId(), b.getLoaiKhach().getTenLoaiKhach(),
                                b.getLoaiKhach().getTuoiTu(), b.getLoaiKhach().getTuoiDen(),
                                b.getLoaiKhach().getHeSoChiemCho(), b.getDonGia()))
                        .toList(),
                d.getDiemDons().stream()
                        .sorted(Comparator.comparing(DotKhoiHanhDiemDon::getGioDon))
                        .map(x -> new DiemDonDto(x.getDiemDon().getDiemDonId(), x.getDiemDon().getTenDiemDon(),
                                x.getDiemDon().getDiaChi(), x.getGioDon(), x.getPhiPhuThuDon()))
                        .toList(),
                !d.getNgayLichTrinhs().isEmpty());
    }

    public static PhuThuDto toPhuThu(PhuThu p) {
        return new PhuThuDto(p.getPhuThuId(), p.getTour() == null ? null : p.getTour().getTourId(),
                p.getTenPhuThu(), p.getCachTinh(), p.getDonGia(), p.getConHieuLuc());
    }

    public static PolicyDto toPolicy(ChinhSachHuy c) {
        if (c == null) return null;
        return new PolicyDto(c.getChinhSachHuyId(), c.getTour() == null ? null : c.getTour().getTourId(),
                c.getTenChinhSach(), c.getPhienBan(), c.getTuNgay(), c.getDenNgay(), c.getNoiDung(),
                c.getMocPhats().stream()
                        .sorted(Comparator.comparing(MocPhatHuy::getSoNgayTruocTu).reversed())
                        .map(m -> new MocDto(m.getSoNgayTruocTu(), m.getSoNgayTruocDen(), m.getPhanTramPhat()))
                        .toList());
    }

    public static NewsDto toNews(TinTuc t) {
        return new NewsDto(t.getTinTucId(), t.getTieuDe(), t.getSlug(), t.getTomTat(), t.getNoiDung(), t.getAnhBia(),
                t.getTrangThai(), t.getLuotXem(), t.getNgayDang(), t.getNgayTao());
    }
}
