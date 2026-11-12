package vn.edu.tvu.vinhlongtour.service;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.*;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Nghiệp vụ xem/tìm kiếm tour phía khách */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourService {
    private final TourRepository tourRepo;
    private final DotKhoiHanhRepository dotRepo;
    private final PhuThuRepository phuThuRepo;
    private final ChinhSachHuyRepository chinhSachRepo;

    /** Tìm kiếm & lọc tour (Q02, Q03, Q07) */
    public Page<TourCard> search(String q, Integer loaiHinhId, Integer khuVucId, Integer dichVuId,
                                 Boolean noiBat, String trangThai, int page, int size) {
        Specification<Tour> spec = (root, query, cb) -> {
            query.distinct(true);
            Predicate p = cb.conjunction();
            if (trangThai != null) p = cb.and(p, cb.equal(root.get("trangThai"), trangThai));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p = cb.and(p, cb.or(cb.like(cb.lower(root.get("tenTour")), like),
                        cb.like(cb.lower(root.get("maTour")), like)));
            }
            if (loaiHinhId != null) {
                Join<Tour, LoaiHinh> j = root.join("loaiHinhs");
                p = cb.and(p, cb.equal(j.get("loaiHinhId"), loaiHinhId));
            }
            if (khuVucId != null) {
                Join<Tour, KhuVuc> j = root.join("khuVucs");
                p = cb.and(p, cb.equal(j.get("khuVucId"), khuVucId));
            }
            if (dichVuId != null) {
                Join<Tour, TourDichVu> j = root.join("dichVus");
                p = cb.and(p, cb.equal(j.get("dichVu").get("dichVuId"), dichVuId),
                        cb.equal(j.get("loaiApDung"), TourDichVu.BAO_GOM));
            }
            if (Boolean.TRUE.equals(noiBat)) p = cb.and(p, cb.isTrue(root.get("noiBat")));
            return p;
        };
        LocalDateTime now = LocalDateTime.now();
        return tourRepo.findAll(spec, PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "noiBat", "tourId")))
                .map(t -> Mapper.toCard(t, dotRepo.findGiaThapNhat(t.getTourId(), now)));
    }

    public TourDetail detailPublic(Integer id) {
        Tour t = tourRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
        if (!"DANG_BAN".equals(t.getTrangThai())) throw ApiException.notFound("Tour hiện không mở bán");
        return toDetail(t, dotRepo.findDangMoBan(id, LocalDateTime.now()));
    }

    public TourDetail detailAdmin(Integer id) {
        Tour t = tourRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
        return toDetail(t, dotRepo.findByTour_TourIdOrderByNgayGioDiAsc(id));
    }

    /** Q13: lịch trình của một đợt (riêng của đợt nếu có, ngược lại dùng lịch trình mẫu) */
    public List<NgayDto> lichTrinhCuaDot(Integer dotId) {
        DotKhoiHanh d = dotRepo.findById(dotId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt"));
        return d.getNgayLichTrinhs().isEmpty()
                ? Mapper.toNgays(d.getTour().getNgayLichTrinhs())
                : Mapper.toNgays(d.getNgayLichTrinhs());
    }

    public DotDto dot(Integer dotId) {
        return Mapper.toDot(dotRepo.findById(dotId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt")));
    }

    private TourDetail toDetail(Tour t, List<DotKhoiHanh> dots) {
        ChinhSachHuy cs = chinhSachRepo.findHieuLuc(t.getTourId(), LocalDate.now()).stream().findFirst().orElse(null);
        return new TourDetail(t.getTourId(), t.getMaTour(), t.getTenTour(), t.getMoTa(), t.getThoiLuong(), t.getSoNgay(),
                t.getTrangThai(), t.getNoiBat(),
                t.getLoaiHinhs().stream().map(l -> new Option(l.getLoaiHinhId(), l.getTenLoaiHinh())).toList(),
                t.getKhuVucs().stream().map(k -> new Option(k.getKhuVucId(), k.getTenKhuVuc())).toList(),
                t.getHinhAnhs().stream().sorted(Comparator.comparing(HinhAnhTour::getThuTuHienThi))
                        .map(h -> new ImageDto(h.getHinhAnhId(), h.getDuongDan(), h.getLaAnhDaiDien(), h.getThuTuHienThi())).toList(),
                t.getDichVus().stream().map(d -> new DichVuDto(d.getDichVu().getDichVuId(), d.getDichVu().getTenDichVu(),
                        d.getDichVu().getBieuTuong(), d.getLoaiApDung(), d.getGhiChu())).toList(),
                Mapper.toNgays(t.getNgayLichTrinhs()),
                dots.stream().map(Mapper::toDot).toList(),
                phuThuRepo.findApDungChoTour(t.getTourId()).stream().map(Mapper::toPhuThu).toList(),
                Mapper.toPolicy(cs));
    }
}
