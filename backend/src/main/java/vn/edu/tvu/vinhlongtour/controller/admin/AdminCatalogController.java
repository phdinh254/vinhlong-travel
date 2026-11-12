package vn.edu.tvu.vinhlongtour.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.AdminDtos.*;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.MocDto;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.PhuThuDto;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.PolicyDto;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.*;
import vn.edu.tvu.vinhlongtour.service.Mapper;

import java.util.List;

/** Danh mục: loại hình, khu vực, dịch vụ, loại khách, điểm đến, điểm đón, phụ thu, chính sách hủy */
@RestController
@RequestMapping("/api/admin/danh-muc")
@RequiredArgsConstructor
@Transactional
public class AdminCatalogController {
    private final LoaiHinhRepository loaiHinhRepo;
    private final KhuVucRepository khuVucRepo;
    private final DichVuRepository dichVuRepo;
    private final LoaiKhachRepository loaiKhachRepo;
    private final DiemDenRepository diemDenRepo;
    private final DiemDonRepository diemDonRepo;
    private final PhuThuRepository phuThuRepo;
    private final ChinhSachHuyRepository chinhSachRepo;
    private final TourRepository tourRepo;

    /* ---- Loại hình (Q02) ---- */
    @GetMapping("/loai-hinh") public List<LoaiHinh> loaiHinhs() { return loaiHinhRepo.findAll(Sort.by("tenLoaiHinh")); }
    @PostMapping("/loai-hinh") public LoaiHinh addLoaiHinh(@RequestBody LoaiHinh x) { x.setLoaiHinhId(null); return loaiHinhRepo.save(require(x.getTenLoaiHinh(), x)); }
    @PutMapping("/loai-hinh/{id}") public LoaiHinh updLoaiHinh(@PathVariable Integer id, @RequestBody LoaiHinh x) { x.setLoaiHinhId(id); return loaiHinhRepo.save(require(x.getTenLoaiHinh(), x)); }
    @DeleteMapping("/loai-hinh/{id}") public void delLoaiHinh(@PathVariable Integer id) { loaiHinhRepo.deleteById(id); }

    /* ---- Khu vực (Q03) ---- */
    @GetMapping("/khu-vuc") public List<KhuVuc> khuVucs() { return khuVucRepo.findAll(Sort.by("khuVucId")); }
    @PostMapping("/khu-vuc") public KhuVuc addKhuVuc(@RequestBody KhuVuc x) { x.setKhuVucId(null); return khuVucRepo.save(require(x.getTenKhuVuc(), x)); }
    @PutMapping("/khu-vuc/{id}") public KhuVuc updKhuVuc(@PathVariable Integer id, @RequestBody KhuVuc x) { x.setKhuVucId(id); return khuVucRepo.save(require(x.getTenKhuVuc(), x)); }
    @DeleteMapping("/khu-vuc/{id}") public void delKhuVuc(@PathVariable Integer id) { khuVucRepo.deleteById(id); }

    /* ---- Dịch vụ (Q07) ---- */
    @GetMapping("/dich-vu") public List<DichVu> dichVus() { return dichVuRepo.findAll(Sort.by("tenDichVu")); }
    @PostMapping("/dich-vu") public DichVu addDichVu(@RequestBody DichVu x) { x.setDichVuId(null); return dichVuRepo.save(require(x.getTenDichVu(), x)); }
    @PutMapping("/dich-vu/{id}") public DichVu updDichVu(@PathVariable Integer id, @RequestBody DichVu x) { x.setDichVuId(id); return dichVuRepo.save(require(x.getTenDichVu(), x)); }
    @DeleteMapping("/dich-vu/{id}") public void delDichVu(@PathVariable Integer id) { dichVuRepo.deleteById(id); }

    /* ---- Loại khách (Q16, Q18, Q19) ---- */
    @GetMapping("/loai-khach") public List<LoaiKhach> loaiKhachs() { return loaiKhachRepo.findAll(Sort.by(Sort.Direction.DESC, "tuoiTu")); }
    @PostMapping("/loai-khach") public LoaiKhach addLoaiKhach(@RequestBody LoaiKhach x) { x.setLoaiKhachId(null); return loaiKhachRepo.save(validLoaiKhach(x)); }
    @PutMapping("/loai-khach/{id}") public LoaiKhach updLoaiKhach(@PathVariable Integer id, @RequestBody LoaiKhach x) { x.setLoaiKhachId(id); return loaiKhachRepo.save(validLoaiKhach(x)); }
    @DeleteMapping("/loai-khach/{id}") public void delLoaiKhach(@PathVariable Integer id) { loaiKhachRepo.deleteById(id); }

    /* ---- Điểm đến (Q04-Q05) ---- */
    @GetMapping("/diem-den") public List<DiemDen> diemDens() { return diemDenRepo.findAll(Sort.by("tenDiemDen")); }
    @PostMapping("/diem-den") public DiemDen addDiemDen(@Valid @RequestBody DiemDenInput r) { return diemDenRepo.save(fill(new DiemDen(), r)); }
    @PutMapping("/diem-den/{id}") public DiemDen updDiemDen(@PathVariable Integer id, @Valid @RequestBody DiemDenInput r) {
        return fill(diemDenRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy điểm đến")), r);
    }
    @DeleteMapping("/diem-den/{id}") public void delDiemDen(@PathVariable Integer id) { diemDenRepo.deleteById(id); }

    /* ---- Điểm đón (Q20) ---- */
    @GetMapping("/diem-don") public List<DiemDon> diemDons() { return diemDonRepo.findAll(Sort.by("tenDiemDon")); }
    @PostMapping("/diem-don") public DiemDon addDiemDon(@Valid @RequestBody DiemDonInput r) { return diemDonRepo.save(fill(new DiemDon(), r)); }
    @PutMapping("/diem-don/{id}") public DiemDon updDiemDon(@PathVariable Integer id, @Valid @RequestBody DiemDonInput r) {
        return fill(diemDonRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy điểm đón")), r);
    }
    @DeleteMapping("/diem-don/{id}") public void delDiemDon(@PathVariable Integer id) { diemDonRepo.deleteById(id); }

    /* ---- Phụ thu (Q21) ---- */
    @GetMapping("/phu-thu") public List<PhuThuDto> phuThus() { return phuThuRepo.findAll().stream().map(Mapper::toPhuThu).toList(); }
    @PostMapping("/phu-thu") public PhuThuDto addPhuThu(@Valid @RequestBody PhuThuInput r) { return Mapper.toPhuThu(phuThuRepo.save(fill(new PhuThu(), r))); }
    @PutMapping("/phu-thu/{id}") public PhuThuDto updPhuThu(@PathVariable Integer id, @Valid @RequestBody PhuThuInput r) {
        return Mapper.toPhuThu(fill(phuThuRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phụ thu")), r));
    }
    @DeleteMapping("/phu-thu/{id}") public void delPhuThu(@PathVariable Integer id) { phuThuRepo.deleteById(id); }

    /* ---- Chính sách hủy (Q37) ---- */
    @GetMapping("/chinh-sach-huy") public List<PolicyDto> policies() {
        return chinhSachRepo.findAll(Sort.by(Sort.Direction.DESC, "tuNgay")).stream().map(Mapper::toPolicy).toList();
    }
    @PostMapping("/chinh-sach-huy") public PolicyDto addPolicy(@Valid @RequestBody PolicyInput r) { return Mapper.toPolicy(chinhSachRepo.save(fill(new ChinhSachHuy(), r))); }
    /** Đơn đã đặt giữ tham chiếu phiên bản cũ; nên tạo phiên bản mới thay vì sửa chính sách đã dùng */
    @PutMapping("/chinh-sach-huy/{id}") public PolicyDto updPolicy(@PathVariable Integer id, @Valid @RequestBody PolicyInput r) {
        return Mapper.toPolicy(fill(chinhSachRepo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy chính sách")), r));
    }
    @DeleteMapping("/chinh-sach-huy/{id}") public void delPolicy(@PathVariable Integer id) { chinhSachRepo.deleteById(id); }

    /* ---- helpers ---- */
    private static <T> T require(String ten, T x) {
        if (ten == null || ten.isBlank()) throw ApiException.badRequest("Tên không được để trống");
        return x;
    }

    private static LoaiKhach validLoaiKhach(LoaiKhach x) {
        require(x.getTenLoaiKhach(), x);
        if (x.getTuoiTu() == null || x.getTuoiDen() == null || x.getTuoiTu() > x.getTuoiDen())
            throw ApiException.badRequest("Khoảng tuổi không hợp lệ");
        if (x.getHeSoChiemCho() == null || (x.getHeSoChiemCho() != 0 && x.getHeSoChiemCho() != 1))
            throw ApiException.badRequest("Hệ số chiếm chỗ phải là 0 hoặc 1");
        return x;
    }

    private KhuVuc kv(Integer id) {
        return id == null ? null : khuVucRepo.findById(id).orElseThrow(() -> ApiException.badRequest("Khu vực không tồn tại"));
    }

    private DiemDen fill(DiemDen d, DiemDenInput r) {
        d.setTenDiemDen(r.tenDiemDen()); d.setKhuVuc(kv(r.khuVucId())); d.setDiaChi(r.diaChi());
        d.setViDo(r.viDo()); d.setKinhDo(r.kinhDo()); d.setMoTa(r.moTa()); d.setHinhAnh(r.hinhAnh());
        return d;
    }

    private DiemDon fill(DiemDon d, DiemDonInput r) {
        d.setTenDiemDon(r.tenDiemDon()); d.setDiaChi(r.diaChi()); d.setKhuVuc(kv(r.khuVucId()));
        return d;
    }

    private PhuThu fill(PhuThu p, PhuThuInput r) {
        p.setTour(r.tourId() == null ? null : tourRepo.findById(r.tourId()).orElseThrow(() -> ApiException.badRequest("Tour không tồn tại")));
        p.setTenPhuThu(r.tenPhuThu()); p.setCachTinh(r.cachTinh()); p.setDonGia(r.donGia());
        p.setConHieuLuc(r.conHieuLuc() == null || r.conHieuLuc());
        return p;
    }

    private ChinhSachHuy fill(ChinhSachHuy c, PolicyInput r) {
        if (r.denNgay() != null && r.denNgay().isBefore(r.tuNgay())) throw ApiException.badRequest("Đến ngày phải sau từ ngày");
        c.setTour(r.tourId() == null ? null : tourRepo.findById(r.tourId()).orElseThrow(() -> ApiException.badRequest("Tour không tồn tại")));
        c.setTenChinhSach(r.tenChinhSach()); c.setPhienBan(r.phienBan()); c.setTuNgay(r.tuNgay());
        c.setDenNgay(r.denNgay()); c.setNoiDung(r.noiDung());
        c.getMocPhats().clear();
        for (MocDto m : r.mocPhats()) {
            if (m.soNgayTruocTu() > m.soNgayTruocDen()) throw ApiException.badRequest("Mốc phạt: 'từ' phải nhỏ hơn hoặc bằng 'đến'");
            MocPhatHuy x = new MocPhatHuy();
            x.setChinhSachHuy(c); x.setSoNgayTruocTu(m.soNgayTruocTu()); x.setSoNgayTruocDen(m.soNgayTruocDen());
            x.setPhanTramPhat(m.phanTramPhat());
            c.getMocPhats().add(x);
        }
        return c;
    }
}
