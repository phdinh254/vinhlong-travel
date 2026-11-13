package vn.edu.tvu.vinhlongtour.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.ContactInput;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.NewsDto;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.*;
import vn.edu.tvu.vinhlongtour.repository.*;
import vn.edu.tvu.vinhlongtour.service.ContentService;
import vn.edu.tvu.vinhlongtour.service.TourService;

import java.util.List;
import java.util.Map;

/** API công khai cho 5 mục: Trang chủ, Giới thiệu, Dịch vụ (tour), Tin tức, Liên hệ */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final TourService tourService;
    private final ContentService contentService;
    private final LoaiHinhRepository loaiHinhRepo;
    private final KhuVucRepository khuVucRepo;
    private final DichVuRepository dichVuRepo;
    private final DiemDenRepository diemDenRepo;
    private final DiemDonRepository diemDonRepo;
    private final PhuThuRepository phuThuRepo;

    @GetMapping("/tours")
    public Page<TourCard> tours(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Integer loaiHinhId,
                                @RequestParam(required = false) Integer khuVucId,
                                @RequestParam(required = false) Integer dichVuId,
                                @RequestParam(required = false) Integer diemDenId,
                                @RequestParam(required = false) Integer soNgay,
                                @RequestParam(required = false) Boolean noiBat,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "9") int size) {
        return tourService.search(q, loaiHinhId, khuVucId, dichVuId, diemDenId, soNgay, noiBat, "DANG_BAN", page, size);
    }

    @GetMapping("/tours/{id}")
    public TourDetail tour(@PathVariable Integer id) {
        return tourService.detailPublic(id);
    }

    @GetMapping("/dots/{id}")
    public DotDto dot(@PathVariable Integer id) {
        return tourService.dot(id);
    }

    @GetMapping("/dots/{id}/lich-trinh")
    public List<NgayDto> lichTrinhDot(@PathVariable Integer id) {
        return tourService.lichTrinhCuaDot(id);
    }

    @GetMapping("/danh-muc")
    public Map<String, Object> danhMuc() {
        return Map.of(
                "loaiHinhs", loaiHinhRepo.findAll(Sort.by("tenLoaiHinh")).stream().map(l -> new Option(l.getLoaiHinhId(), l.getTenLoaiHinh())).toList(),
                "khuVucs", khuVucRepo.findAll(Sort.by("khuVucId")).stream().map(k -> new Option(k.getKhuVucId(), k.getTenKhuVuc())).toList(),
                "dichVus", dichVuRepo.findAll(Sort.by("tenDichVu")).stream().map(d -> new Option(d.getDichVuId(), d.getTenDichVu())).toList(),
                "diemDens", diemDenRepo.findAll(Sort.by("tenDiemDen")),
                // Trang Dịch vụ: điểm đón và phụ thu tùy chọn chung
                "diemDons", diemDonRepo.findAll(Sort.by("diemDonId")).stream()
                        .map(d -> Map.of("id", d.getDiemDonId(), "ten", d.getTenDiemDon(), "diaChi", d.getDiaChi() == null ? "" : d.getDiaChi())).toList(),
                "phuThus", phuThuRepo.findChungDangHieuLuc().stream()
                        .map(p -> Map.of("ten", p.getTenPhuThu(), "cachTinh", p.getCachTinh(), "donGia", p.getDonGia())).toList());
    }

    @GetMapping("/cau-hinh")
    public Map<String, String> cauHinh() {
        return contentService.pageConfig();
    }

    @GetMapping("/tin-tuc")
    public Page<NewsDto> tinTuc(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "9") int size,
                                @RequestParam(required = false) String chuyenMuc) {
        return contentService.publishedNews(page, size, chuyenMuc);
    }

    @GetMapping("/tin-tuc/{slug}")
    public NewsDto tinTucChiTiet(@PathVariable String slug) {
        return contentService.newsBySlug(slug);
    }

    @PostMapping("/lien-he")
    public Map<String, String> lienHe(@Valid @RequestBody ContactInput r) {
        contentService.submitContact(r);
        return Map.of("message", "Cảm ơn bạn đã liên hệ, chúng tôi sẽ phản hồi sớm nhất");
    }
}
