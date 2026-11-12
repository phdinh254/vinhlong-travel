package vn.edu.tvu.vinhlongtour.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.*;
import vn.edu.tvu.vinhlongtour.entity.DonDatTour;
import vn.edu.tvu.vinhlongtour.entity.LienHe;
import vn.edu.tvu.vinhlongtour.entity.YeuCauHuy;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;
import vn.edu.tvu.vinhlongtour.security.CurrentUser;
import vn.edu.tvu.vinhlongtour.service.BookingService;
import vn.edu.tvu.vinhlongtour.service.ContentService;
import vn.edu.tvu.vinhlongtour.service.FileStorageService;

import java.util.List;
import java.util.Map;

/** Tin tức, Liên hệ, Giới thiệu (cấu hình trang), Dashboard, Người dùng, Upload */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {
    private final ContentService contentService;
    private final FileStorageService fileStorage;
    private final BookingService bookingService;
    private final TourRepository tourRepo;
    private final DonDatTourRepository donRepo;
    private final LienHeRepository lienHeRepo;
    private final YeuCauHuyRepository yeuCauRepo;
    private final TaiKhoanRepository taiKhoanRepo;

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        return new Dashboard(tourRepo.count(), donRepo.count(),
                donRepo.countByTrangThai(DonDatTour.CHO_THANH_TOAN), donRepo.countByTrangThai(DonDatTour.DA_COC),
                donRepo.countByTrangThai(DonDatTour.DA_THANH_TOAN), donRepo.tongDoanhThuDaThu(),
                lienHeRepo.countByTrangThai("MOI"),
                yeuCauRepo.findAll().stream().filter(y -> YeuCauHuy.CHO_XU_LY.equals(y.getKetQua())).count(),
                donRepo.findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "ngayDat"))).map(bookingService::toSummary).getContent());
    }

    /* ---- Tin tức ---- */
    @GetMapping("/tin-tuc") public List<NewsDto> news() { return contentService.allNews(); }
    @PostMapping("/tin-tuc") public NewsDto addNews(@Valid @RequestBody NewsSaveRequest r) { return contentService.saveNews(null, r); }
    @PutMapping("/tin-tuc/{id}") public NewsDto updNews(@PathVariable Integer id, @Valid @RequestBody NewsSaveRequest r) { return contentService.saveNews(id, r); }
    @DeleteMapping("/tin-tuc/{id}") public void delNews(@PathVariable Integer id) { contentService.deleteNews(id); }

    /* ---- Liên hệ ---- */
    @GetMapping("/lien-he") public List<LienHe> contacts() { return contentService.contacts(); }
    @PutMapping("/lien-he/{id}") public void markContact(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String tt = body.getOrDefault("trangThai", "DA_XU_LY");
        if (!tt.equals("MOI") && !tt.equals("DA_XU_LY")) throw ApiException.badRequest("Trạng thái không hợp lệ");
        contentService.markContact(id, tt);
    }

    /* ---- Giới thiệu / thông tin liên hệ ---- */
    @GetMapping("/cau-hinh") public Map<String, String> config() { return contentService.pageConfig(); }
    @PutMapping("/cau-hinh") public Map<String, String> saveConfig(@RequestBody Map<String, String> body) { return contentService.savePageConfig(body); }

    /* ---- Người dùng ---- */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserDto> users() {
        return taiKhoanRepo.findAll(Sort.by(Sort.Direction.DESC, "ngayTao")).stream()
                .map(t -> new UserDto(t.getTaiKhoanId(), t.getEmail(), t.getHoTen(), t.getVaiTro().getMaVaiTro(),
                        t.getNhaCungCap(), t.getTrangThai(), t.getNgayTao())).toList();
    }

    @PutMapping("/users/{id}/trang-thai")
    @Transactional
    public void lockUser(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        if (id.equals(CurrentUser.require().taiKhoanId())) throw ApiException.badRequest("Không thể tự khóa tài khoản của mình");
        String tt = body.get("trangThai");
        if (!"HOAT_DONG".equals(tt) && !"KHOA".equals(tt)) throw ApiException.badRequest("Trạng thái không hợp lệ");
        taiKhoanRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản")).setTrangThai(tt);
    }

    /* ---- Upload ảnh ---- */
    @PostMapping("/files")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", fileStorage.store(file));
    }
}
