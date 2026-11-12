package vn.edu.tvu.vinhlongtour.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.entity.*;
import vn.edu.tvu.vinhlongtour.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Khởi tạo khi ứng dụng chạy:
 *  - Luôn đảm bảo có vai trò ADMIN/KHACH_HANG và 1 tài khoản admin.
 *  - Nếu app.demo-data=true và chưa có tour: sinh dữ liệu mẫu (dùng cho profile dev-h2).
 *    Với MySQL (XAMPP), dữ liệu mẫu nằm ở database/02_seed.sql.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final VaiTroRepository vaiTroRepo;
    private final TaiKhoanRepository taiKhoanRepo;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final TourRepository tourRepo;
    private final LoaiHinhRepository loaiHinhRepo;
    private final KhuVucRepository khuVucRepo;
    private final DichVuRepository dichVuRepo;
    private final DiemDenRepository diemDenRepo;
    private final LoaiKhachRepository loaiKhachRepo;
    private final DiemDonRepository diemDonRepo;
    private final DotKhoiHanhRepository dotRepo;
    private final PhuThuRepository phuThuRepo;
    private final ChinhSachHuyRepository chinhSachRepo;
    private final CauHinhTrangRepository cauHinhRepo;
    private final TinTucRepository tinTucRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        VaiTro admin = ensureRole(VaiTro.ADMIN, "Quản trị viên");
        ensureRole(VaiTro.KHACH_HANG, "Khách hàng");
        if (taiKhoanRepo.findByEmailIgnoreCase(props.admin().email()).isEmpty()) {
            TaiKhoan tk = new TaiKhoan();
            tk.setEmail(props.admin().email());
            tk.setHoTen(props.admin().name());
            tk.setMatKhauHash(encoder.encode(props.admin().password()));
            tk.setVaiTro(admin);
            taiKhoanRepo.save(tk);
            log.warn("Đã tạo tài khoản admin {} — hãy đổi mật khẩu sau khi đăng nhập", props.admin().email());
        }
        if (props.demoData() && tourRepo.count() == 0) seedDemo();
    }

    private VaiTro ensureRole(String ma, String ten) {
        return vaiTroRepo.findByMaVaiTro(ma).orElseGet(() -> {
            VaiTro v = new VaiTro();
            v.setMaVaiTro(ma);
            v.setTenVaiTro(ten);
            return vaiTroRepo.save(v);
        });
    }

    private void seedDemo() {
        log.info("Sinh dữ liệu mẫu...");
        KhuVuc kvVL = khuVuc("Khu vực Vĩnh Long (cũ)");
        KhuVuc kvBT = khuVuc("Khu vực Bến Tre (cũ)");
        KhuVuc kvTV = khuVuc("Khu vực Trà Vinh (cũ)");
        LoaiHinh sinhThai = loaiHinh("Sinh thái miệt vườn");
        LoaiHinh vanHoa = loaiHinh("Văn hóa - Lịch sử");
        LoaiHinh langNghe = loaiHinh("Làng nghề truyền thống");
        LoaiHinh bien = loaiHinh("Biển - Nghỉ dưỡng");
        loaiHinh("Ẩm thực");
        LoaiKhach nl = loaiKhach("Người lớn", 12, 150, 1);
        LoaiKhach te = loaiKhach("Trẻ em", 5, 11, 1);
        LoaiKhach eb = loaiKhach("Em bé", 0, 4, 0);
        List<DichVu> dv = List.of(dichVu("Xe đưa đón"), dichVu("Hướng dẫn viên"), dichVu("Vé tham quan"),
                dichVu("Bữa ăn theo chương trình"), dichVu("Bảo hiểm du lịch"), dichVu("Chi phí cá nhân"), dichVu("Thuế VAT"));
        DiemDen anBinh = diemDen("Cù lao An Bình", kvVL);
        DiemDen mangThit = diemDen("Làng nghề gạch gốm Mang Thít", kvVL);
        DiemDen conPhung = diemDen("Cồn Phụng", kvBT);
        DiemDen aoBaOm = diemDen("Ao Bà Om", kvTV);
        DiemDen baDong = diemDen("Biển Ba Động", kvTV);
        DiemDon ddVL = diemDon("Bến xe Vĩnh Long", kvVL);
        DiemDon ddHCM = diemDon("TP. Hồ Chí Minh - Quận 1", null);

        Tour t1 = tour("VL-AB-1N", "Cù lao An Bình – Miệt vườn Vĩnh Long 1 ngày", "1 ngày", 1,
                Set.of(sinhThai, langNghe), Set.of(kvVL), dv, true);
        ngay(t1, 1, "Khám phá cù lao An Bình", new Object[][]{
                {"DI_CHUYEN", "07:00", "Đón khách, khởi hành", null},
                {"THAM_QUAN", "08:30", "Đi xuồng kênh rạch, tham quan vườn trái cây", anBinh},
                {"AN_UONG", "11:30", "Ăn trưa đặc sản miệt vườn", null},
                {"THAM_QUAN", "14:00", "Tham quan làng nghề gạch gốm Mang Thít", mangThit}});
        Tour t2 = tour("BT-DUA-2N", "Xứ dừa Bến Tre – Cồn Phụng 2 ngày 1 đêm", "2 ngày 1 đêm", 2,
                Set.of(sinhThai, langNghe), Set.of(kvBT), dv, true);
        ngay(t2, 1, "Cồn Phụng – Làng nghề dừa", new Object[][]{{"THAM_QUAN", "08:30", "Tham quan Cồn Phụng", conPhung}});
        ngay(t2, 2, "Tự do – Trở về", new Object[][]{{"DI_CHUYEN", "14:00", "Trả khách", null}});
        Tour t3 = tour("TV-KHMER-2N", "Văn hóa Khmer Trà Vinh – Biển Ba Động 2 ngày 1 đêm", "2 ngày 1 đêm", 2,
                Set.of(vanHoa, bien), Set.of(kvTV), dv, false);
        ngay(t3, 1, "Ao Bà Om – Chùa Âng", new Object[][]{{"THAM_QUAN", "09:00", "Tham quan Ao Bà Om", aoBaOm}});
        ngay(t3, 2, "Biển Ba Động", new Object[][]{{"THAM_QUAN", "08:00", "Tắm biển Ba Động", baDong}});

        int i = 0;
        for (Tour t : List.of(t1, t2, t3)) {
            for (int k = 1; k <= 2; k++) {
                LocalDateTime di = LocalDate.now().plusDays(14L * k).atTime(7, 0);
                DotKhoiHanh d = new DotKhoiHanh();
                d.setTour(t);
                d.setMaDot(t.getMaTour() + "-" + di.format(DateTimeFormatter.ofPattern("yyMMdd")));
                d.setNgayGioDi(di);
                d.setNgayGioVe(di.plusDays(t.getSoNgay() - 1).withHour(17));
                d.setSucChuaToiDa(30);
                d.setHanNhanKhach(di.minusDays(2));
                long base = new long[]{650_000, 1_590_000, 1_490_000}[i];
                gia(d, nl, base);
                gia(d, te, base * 7 / 10);
                gia(d, eb, 0);
                diemDonDot(d, ddVL, di, 0);
                diemDonDot(d, ddHCM, di.minusHours(2), 150_000);
                dotRepo.save(d);
            }
            i++;
        }

        PhuThu p1 = new PhuThu(); p1.setTenPhuThu("Phòng đơn"); p1.setCachTinh(PhuThu.THEO_SO_LUONG); p1.setDonGia(BigDecimal.valueOf(250_000));
        PhuThu p2 = new PhuThu(); p2.setTenPhuThu("Bảo hiểm nâng cao"); p2.setCachTinh(PhuThu.THEO_NGUOI); p2.setDonGia(BigDecimal.valueOf(30_000));
        phuThuRepo.saveAll(List.of(p1, p2));

        ChinhSachHuy cs = new ChinhSachHuy();
        cs.setTenChinhSach("Chính sách hủy chung");
        cs.setTuNgay(LocalDate.of(2025, 1, 1));
        cs.setNoiDung("Áp dụng cho mọi tour không có chính sách riêng.");
        moc(cs, 15, 9999, 0); moc(cs, 7, 14, 30); moc(cs, 3, 6, 50); moc(cs, 0, 2, 100);
        chinhSachRepo.save(cs);

        cauHinh("GIOI_THIEU", "<p>Chúng tôi chuyên tổ chức các tour khám phá tỉnh Vĩnh Long (mới) – vùng đất hội tụ miệt vườn sông nước, xứ dừa và văn hóa Khmer.</p>");
        cauHinh("HOTLINE", "0000 000 000");
        cauHinh("EMAIL", "lienhe@example.com");
        cauHinh("DIA_CHI", "Vĩnh Long");

        TinTuc tt = new TinTuc();
        tt.setTieuDe("Khám phá tỉnh Vĩnh Long mới sau hợp nhất");
        tt.setSlug("kham-pha-tinh-vinh-long-moi");
        tt.setTomTat("Gợi ý các điểm đến tiêu biểu của ba khu vực Vĩnh Long, Bến Tre, Trà Vinh.");
        tt.setNoiDung("<p>Bài viết mẫu – thay bằng nội dung thật.</p>");
        tt.setTrangThai("XUAT_BAN");
        tt.setNgayDang(LocalDateTime.now());
        tinTucRepo.save(tt);
    }

    private KhuVuc khuVuc(String ten) { KhuVuc k = new KhuVuc(); k.setTenKhuVuc(ten); return khuVucRepo.save(k); }
    private LoaiHinh loaiHinh(String ten) { LoaiHinh l = new LoaiHinh(); l.setTenLoaiHinh(ten); return loaiHinhRepo.save(l); }
    private DichVu dichVu(String ten) { DichVu d = new DichVu(); d.setTenDichVu(ten); return dichVuRepo.save(d); }
    private DiemDen diemDen(String ten, KhuVuc kv) { DiemDen d = new DiemDen(); d.setTenDiemDen(ten); d.setKhuVuc(kv); return diemDenRepo.save(d); }
    private DiemDon diemDon(String ten, KhuVuc kv) { DiemDon d = new DiemDon(); d.setTenDiemDon(ten); d.setKhuVuc(kv); return diemDonRepo.save(d); }

    private LoaiKhach loaiKhach(String ten, int tu, int den, int heSo) {
        LoaiKhach l = new LoaiKhach(); l.setTenLoaiKhach(ten); l.setTuoiTu(tu); l.setTuoiDen(den); l.setHeSoChiemCho(heSo);
        return loaiKhachRepo.save(l);
    }

    private Tour tour(String ma, String ten, String thoiLuong, int soNgay, Set<LoaiHinh> lh, Set<KhuVuc> kv,
                      List<DichVu> dv, boolean noiBat) {
        Tour t = new Tour();
        t.setMaTour(ma); t.setTenTour(ten); t.setThoiLuong(thoiLuong); t.setSoNgay(soNgay);
        t.setMoTa("Tour mẫu: " + ten + ". Thay nội dung mô tả thật trong trang quản trị.");
        t.setTrangThai("DANG_BAN"); t.setNoiBat(noiBat);
        t.getLoaiHinhs().addAll(lh); t.getKhuVucs().addAll(kv);
        for (int i = 0; i < dv.size(); i++) {
            TourDichVu td = new TourDichVu();
            td.setTour(t); td.setDichVu(dv.get(i));
            td.setLoaiApDung(i < 5 ? TourDichVu.BAO_GOM : TourDichVu.KHONG_BAO_GOM);
            t.getDichVus().add(td);
        }
        return tourRepo.save(t);
    }

    private void ngay(Tour t, int thu, String tieuDe, Object[][] hoatDongs) {
        NgayLichTrinh n = new NgayLichTrinh();
        n.setTour(t); n.setNgayThuMay(thu); n.setTieuDeNgay(tieuDe);
        int i = 1;
        for (Object[] h : hoatDongs) {
            HoatDongLichTrinh hd = new HoatDongLichTrinh();
            hd.setNgayLichTrinh(n);
            hd.setLoaiHoatDong((String) h[0]);
            hd.setGioBatDau(LocalTime.parse((String) h[1]));
            hd.setNoiDung((String) h[2]);
            hd.setDiemDen((DiemDen) h[3]);
            hd.setThuTuHienThi(i++);
            n.getHoatDongs().add(hd);
        }
        t.getNgayLichTrinhs().add(n);
        tourRepo.flush();
    }

    private void gia(DotKhoiHanh d, LoaiKhach lk, long donGia) {
        BangGiaDot b = new BangGiaDot(); b.setDotKhoiHanh(d); b.setLoaiKhach(lk); b.setDonGia(BigDecimal.valueOf(donGia));
        d.getBangGias().add(b);
    }

    private void diemDonDot(DotKhoiHanh d, DiemDon dd, LocalDateTime gio, long phi) {
        DotKhoiHanhDiemDon x = new DotKhoiHanhDiemDon(); x.setDotKhoiHanh(d); x.setDiemDon(dd); x.setGioDon(gio);
        x.setPhiPhuThuDon(BigDecimal.valueOf(phi));
        d.getDiemDons().add(x);
    }

    private void moc(ChinhSachHuy cs, int tu, int den, int pt) {
        MocPhatHuy m = new MocPhatHuy(); m.setChinhSachHuy(cs); m.setSoNgayTruocTu(tu); m.setSoNgayTruocDen(den);
        m.setPhanTramPhat(BigDecimal.valueOf(pt));
        cs.getMocPhats().add(m);
    }

    private void cauHinh(String k, String v) {
        CauHinhTrang c = new CauHinhTrang(); c.setKhoaCauHinh(k); c.setGiaTri(v); cauHinhRepo.save(c);
    }
}
