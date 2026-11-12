package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Quản trị tour, lịch trình, đợt khởi hành, bảng giá, điểm đón (Q01–Q20) */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminTourService {
    private final TourRepository tourRepo;
    private final LoaiHinhRepository loaiHinhRepo;
    private final KhuVucRepository khuVucRepo;
    private final DichVuRepository dichVuRepo;
    private final DiemDenRepository diemDenRepo;
    private final DotKhoiHanhRepository dotRepo;
    private final LoaiKhachRepository loaiKhachRepo;
    private final DiemDonRepository diemDonRepo;
    private final DonDatTourRepository donRepo;
    private final TourService tourService;

    public TourDetail saveTour(Integer id, TourSaveRequest r) {
        Tour t;
        if (id == null) {
            if (tourRepo.existsByMaTourIgnoreCase(r.maTour())) throw ApiException.conflict("Mã tour đã tồn tại");
            t = new Tour();
        } else {
            t = tourRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
            if (!t.getMaTour().equalsIgnoreCase(r.maTour()) && tourRepo.existsByMaTourIgnoreCase(r.maTour()))
                throw ApiException.conflict("Mã tour đã tồn tại");
            t.setNgayCapNhat(LocalDateTime.now());
        }
        t.setMaTour(r.maTour().trim());
        t.setTenTour(r.tenTour().trim());
        t.setMoTa(r.moTa());
        t.setThoiLuong(r.thoiLuong());
        t.setSoNgay(r.soNgay());
        t.setTrangThai(r.trangThai());
        t.setNoiBat(Boolean.TRUE.equals(r.noiBat()));
        Set<LoaiHinh> loaiHinhs = new HashSet<>();
        if (r.loaiHinhIds() != null) loaiHinhs.addAll(loaiHinhRepo.findAllById(r.loaiHinhIds()));
        t.setLoaiHinhs(loaiHinhs);
        Set<KhuVuc> khuVucs = new HashSet<>();
        if (r.khuVucIds() != null) khuVucs.addAll(khuVucRepo.findAllById(r.khuVucIds()));
        t.setKhuVucs(khuVucs);

        // Xóa con cũ và flush TRƯỚC khi thêm con mới để không vi phạm ràng buộc UNIQUE
        t.getHinhAnhs().clear();
        t.getDichVus().clear();
        t.getNgayLichTrinhs().clear();
        tourRepo.saveAndFlush(t);

        // Q06: ảnh - đảm bảo đúng 1 ảnh đại diện
        if (r.hinhAnhs() != null) {
            int i = 0;
            boolean coDaiDien = r.hinhAnhs().stream().anyMatch(x -> Boolean.TRUE.equals(x.laAnhDaiDien()));
            for (ImageInput in : r.hinhAnhs()) {
                HinhAnhTour h = new HinhAnhTour();
                h.setTour(t);
                h.setDuongDan(in.duongDan());
                h.setThuTuHienThi(in.thuTuHienThi() != null ? in.thuTuHienThi() : i);
                h.setLaAnhDaiDien(coDaiDien ? Boolean.TRUE.equals(in.laAnhDaiDien()) : i == 0);
                t.getHinhAnhs().add(h);
                i++;
            }
        }

        // Q07
        if (r.dichVus() != null) {
            for (DichVuInput in : r.dichVus()) {
                if (t.getDichVus().stream().anyMatch(x -> x.getDichVu().getDichVuId().equals(in.dichVuId()))) continue;
                TourDichVu td = new TourDichVu();
                td.setTour(t);
                td.setDichVu(dichVuRepo.findById(in.dichVuId()).orElseThrow(() -> ApiException.badRequest("Dịch vụ không tồn tại")));
                td.setLoaiApDung(in.loaiApDung());
                td.setGhiChu(in.ghiChu());
                t.getDichVus().add(td);
            }
        }

        // Q08-Q12: lịch trình mẫu
        if (r.lichTrinh() != null) {
            for (NgayInput n : r.lichTrinh()) t.getNgayLichTrinhs().add(buildNgay(n, t, null));
        }
        tourRepo.flush(); // entity đang được quản lý: cascade PERSIST cho các con mới
        return tourService.detailAdmin(t.getTourId());
    }

    public void deleteTour(Integer id) {
        Tour t = tourRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
        if (!dotRepo.findByTour_TourIdOrderByNgayGioDiAsc(id).isEmpty()) {
            throw ApiException.conflict("Tour đã có đợt khởi hành, hãy chuyển trạng thái sang NGUNG_BAN thay vì xóa");
        }
        tourRepo.delete(t);
    }

    /** Q14-Q20 (+Q13 sao chép lịch trình mẫu sang đợt) */
    public DotDto saveDot(Integer id, DotSaveRequest r) {
        if (r.ngayGioVe().isBefore(r.ngayGioDi())) throw ApiException.badRequest("Ngày về phải sau ngày đi");
        if (r.hanNhanKhach().isAfter(r.ngayGioDi())) throw ApiException.badRequest("Hạn nhận khách phải trước giờ khởi hành");
        Tour tour = tourRepo.findById(r.tourId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
        DotKhoiHanh d;
        if (id == null) {
            if (dotRepo.existsByMaDotIgnoreCase(r.maDot())) throw ApiException.conflict("Mã đợt đã tồn tại");
            d = new DotKhoiHanh();
            d.setTour(tour);
        } else {
            d = dotRepo.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
            if (!d.getMaDot().equalsIgnoreCase(r.maDot()) && dotRepo.existsByMaDotIgnoreCase(r.maDot()))
                throw ApiException.conflict("Mã đợt đã tồn tại");
            if (r.sucChuaToiDa() < d.getSoChoDaDat())
                throw ApiException.badRequest("Sức chứa không được nhỏ hơn số chỗ đã đặt (" + d.getSoChoDaDat() + ")");
        }
        d.setMaDot(r.maDot().trim());
        d.setNgayGioDi(r.ngayGioDi());
        d.setNgayGioVe(r.ngayGioVe());
        d.setSucChuaToiDa(r.sucChuaToiDa());
        d.setHanNhanKhach(r.hanNhanKhach());
        d.setTrangThai(r.trangThai() != null ? r.trangThai() : "MO_BAN");
        if ("MO_BAN".equals(d.getTrangThai()) && d.soChoConLai() <= 0) d.setTrangThai("HET_CHO");

        d.getBangGias().clear();
        d.getDiemDons().clear();
        dotRepo.saveAndFlush(d);
        if (r.bangGia() != null) {
            for (GiaInput g : r.bangGia()) {
                BangGiaDot b = new BangGiaDot();
                b.setDotKhoiHanh(d);
                b.setLoaiKhach(loaiKhachRepo.findById(g.loaiKhachId()).orElseThrow(() -> ApiException.badRequest("Loại khách không tồn tại")));
                b.setDonGia(g.donGia());
                d.getBangGias().add(b);
            }
        }
        if (r.diemDons() != null) {
            for (DiemDonInput x : r.diemDons()) {
                DotKhoiHanhDiemDon dd = new DotKhoiHanhDiemDon();
                dd.setDotKhoiHanh(d);
                dd.setDiemDon(diemDonRepo.findById(x.diemDonId()).orElseThrow(() -> ApiException.badRequest("Điểm đón không tồn tại")));
                dd.setGioDon(x.gioDon());
                dd.setPhiPhuThuDon(x.phiPhuThuDon() == null ? BigDecimal.ZERO : x.phiPhuThuDon());
                d.getDiemDons().add(dd);
            }
        }
        if (id == null && Boolean.TRUE.equals(r.saoChepLichTrinh())) {
            for (NgayLichTrinh mau : tour.getNgayLichTrinhs()) d.getNgayLichTrinhs().add(copyNgay(mau, d));
        }
        dotRepo.flush();
        return Mapper.toDot(d);
    }

    /** Q13: cập nhật lịch trình riêng của đợt. Danh sách rỗng = quay về dùng lịch trình mẫu */
    public List<NgayDto> saveLichTrinhDot(Integer dotId, List<NgayInput> ngays) {
        DotKhoiHanh d = dotRepo.findById(dotId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
        d.getNgayLichTrinhs().clear();
        dotRepo.saveAndFlush(d);
        if (ngays != null) for (NgayInput n : ngays) d.getNgayLichTrinhs().add(buildNgay(n, null, d));
        dotRepo.flush();
        return Mapper.toNgays(d.getNgayLichTrinhs());
    }

    public void deleteDot(Integer id) {
        DotKhoiHanh d = dotRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
        boolean coDon = donRepo.count((root, q, cb) -> cb.equal(root.get("dotKhoiHanh").get("dotKhoiHanhId"), id)) > 0;
        if (coDon) throw ApiException.conflict("Đợt đã có đơn đặt, hãy chuyển trạng thái sang DONG hoặc HUY");
        dotRepo.delete(d);
    }

    private NgayLichTrinh buildNgay(NgayInput n, Tour tour, DotKhoiHanh dot) {
        NgayLichTrinh ng = new NgayLichTrinh();
        ng.setTour(tour);
        ng.setDotKhoiHanh(dot);
        ng.setNgayThuMay(n.ngayThuMay());
        ng.setTieuDeNgay(n.tieuDeNgay());
        if (n.hoatDongs() != null) {
            int i = 1;
            for (HoatDongInput h : n.hoatDongs()) {
                HoatDongLichTrinh hd = new HoatDongLichTrinh();
                hd.setNgayLichTrinh(ng);
                hd.setLoaiHoatDong(h.loaiHoatDong());
                hd.setGioBatDau(h.gioBatDau());
                hd.setGioKetThuc(h.gioKetThuc());
                hd.setNoiDung(h.noiDung());
                hd.setThuTuHienThi(h.thuTuHienThi() != null ? h.thuTuHienThi() : i);
                hd.setDiemDen(h.diemDenId() == null ? null : diemDenRepo.findById(h.diemDenId())
                        .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại")));
                ng.getHoatDongs().add(hd);
                i++;
            }
        }
        return ng;
    }

    private NgayLichTrinh copyNgay(NgayLichTrinh mau, DotKhoiHanh dot) {
        NgayLichTrinh ng = new NgayLichTrinh();
        ng.setDotKhoiHanh(dot);
        ng.setNgayThuMay(mau.getNgayThuMay());
        ng.setTieuDeNgay(mau.getTieuDeNgay());
        mau.getHoatDongs().stream().filter(Objects::nonNull).forEach(h -> {
            HoatDongLichTrinh hd = new HoatDongLichTrinh();
            hd.setNgayLichTrinh(ng);
            hd.setLoaiHoatDong(h.getLoaiHoatDong());
            hd.setGioBatDau(h.getGioBatDau());
            hd.setGioKetThuc(h.getGioKetThuc());
            hd.setNoiDung(h.getNoiDung());
            hd.setThuTuHienThi(h.getThuTuHienThi());
            hd.setDiemDen(h.getDiemDen());
            ng.getHoatDongs().add(hd);
        });
        return ng;
    }
}
