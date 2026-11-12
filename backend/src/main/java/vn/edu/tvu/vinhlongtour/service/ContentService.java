package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.*;
import vn.edu.tvu.vinhlongtour.entity.CauHinhTrang;
import vn.edu.tvu.vinhlongtour.entity.LienHe;
import vn.edu.tvu.vinhlongtour.entity.TinTuc;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.CauHinhTrangRepository;
import vn.edu.tvu.vinhlongtour.repository.LienHeRepository;
import vn.edu.tvu.vinhlongtour.repository.TinTucRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Giới thiệu (cấu hình trang), Tin tức, Liên hệ */
@Service
@RequiredArgsConstructor
public class ContentService {
    private final TinTucRepository tinTucRepo;
    private final CauHinhTrangRepository cauHinhRepo;
    private final LienHeRepository lienHeRepo;
    private final BookingSupport support;

    /* ---------- Cấu hình trang ---------- */
    @Transactional(readOnly = true)
    public Map<String, String> pageConfig() {
        Map<String, String> m = new LinkedHashMap<>();
        cauHinhRepo.findAll().forEach(c -> m.put(c.getKhoaCauHinh(), c.getGiaTri()));
        return m;
    }

    @Transactional
    public Map<String, String> savePageConfig(Map<String, String> values) {
        values.forEach((k, v) -> {
            if (k == null || k.isBlank() || k.length() > 50) throw ApiException.badRequest("Khóa cấu hình không hợp lệ");
            CauHinhTrang c = cauHinhRepo.findById(k).orElseGet(() -> {
                CauHinhTrang n = new CauHinhTrang();
                n.setKhoaCauHinh(k);
                return n;
            });
            c.setGiaTri(v);
            c.setNgayCapNhat(LocalDateTime.now());
            cauHinhRepo.save(c);
        });
        return pageConfig();
    }

    /* ---------- Tin tức ---------- */
    @Transactional(readOnly = true)
    public Page<NewsDto> publishedNews(int page, int size) {
        return tinTucRepo.findByTrangThaiOrderByNgayDangDesc("XUAT_BAN", PageRequest.of(page, Math.min(size, 50)))
                .map(Mapper::toNews);
    }

    @Transactional
    public NewsDto newsBySlug(String slug) {
        TinTuc t = tinTucRepo.findBySlugAndTrangThai(slug, "XUAT_BAN").orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
        t.setLuotXem(t.getLuotXem() + 1);
        return Mapper.toNews(t);
    }

    @Transactional(readOnly = true)
    public List<NewsDto> allNews() {
        return tinTucRepo.findAll(Sort.by(Sort.Direction.DESC, "ngayTao")).stream().map(Mapper::toNews).toList();
    }

    @Transactional
    public NewsDto saveNews(Integer id, NewsSaveRequest r) {
        TinTuc t = id == null ? new TinTuc() : tinTucRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
        String slug = (r.slug() == null || r.slug().isBlank()) ? slugify(r.tieuDe()) : slugify(r.slug());
        if (!slug.equals(t.getSlug()) && tinTucRepo.existsBySlug(slug)) slug = slug + "-" + System.currentTimeMillis() % 100000;
        t.setTieuDe(r.tieuDe());
        t.setSlug(slug);
        t.setTomTat(r.tomTat());
        t.setNoiDung(r.noiDung());
        t.setAnhBia(r.anhBia());
        if ("XUAT_BAN".equals(r.trangThai()) && t.getNgayDang() == null) t.setNgayDang(LocalDateTime.now());
        t.setTrangThai(r.trangThai());
        if (t.getTacGia() == null) t.setTacGia(support.currentAccount());
        return Mapper.toNews(tinTucRepo.save(t));
    }

    @Transactional
    public void deleteNews(Integer id) {
        tinTucRepo.deleteById(id);
    }

    /* ---------- Liên hệ ---------- */
    @Transactional
    public void submitContact(ContactInput r) {
        LienHe l = new LienHe();
        l.setHoTen(r.hoTen());
        l.setEmail(r.email());
        l.setSoDienThoai(r.soDienThoai());
        l.setTieuDe(r.tieuDe());
        l.setNoiDung(r.noiDung());
        lienHeRepo.save(l);
    }

    @Transactional(readOnly = true)
    public List<LienHe> contacts() {
        return lienHeRepo.findAllByOrderByNgayGuiDesc();
    }

    @Transactional
    public void markContact(Integer id, String trangThai) {
        LienHe l = lienHeRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy liên hệ"));
        l.setTrangThai(trangThai);
    }

    /** Tạo slug không dấu: "Khám phá Vĩnh Long" -> "kham-pha-vinh-long" */
    public static String slugify(String s) {
        String n = Normalizer.normalize(s.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase()
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.length() > 300 ? n.substring(0, 300) : n;
    }
}
