package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.dto.AuthDtos.*;
import vn.edu.tvu.vinhlongtour.entity.KhachHang;
import vn.edu.tvu.vinhlongtour.entity.TaiKhoan;
import vn.edu.tvu.vinhlongtour.entity.VaiTro;
import vn.edu.tvu.vinhlongtour.exception.ApiException;
import vn.edu.tvu.vinhlongtour.repository.KhachHangRepository;
import vn.edu.tvu.vinhlongtour.repository.TaiKhoanRepository;
import vn.edu.tvu.vinhlongtour.repository.VaiTroRepository;
import vn.edu.tvu.vinhlongtour.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TaiKhoanRepository taiKhoanRepo;
    private final VaiTroRepository vaiTroRepo;
    private final KhachHangRepository khachHangRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final GoogleTokenService googleTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (taiKhoanRepo.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        TaiKhoan tk = new TaiKhoan();
        tk.setEmail(email);
        tk.setHoTen(req.hoTen().trim());
        tk.setMatKhauHash(encoder.encode(req.matKhau()));
        tk.setNhaCungCap("LOCAL");
        tk.setVaiTro(role(VaiTro.KHACH_HANG));
        taiKhoanRepo.save(tk);
        KhachHang kh = taoHoSo(tk);
        kh.setSoDienThoai(req.soDienThoai());
        return response(tk);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        TaiKhoan tk = taiKhoanRepo.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));
        if (tk.getMatKhauHash() == null) {
            throw ApiException.unauthorized("Tài khoản này đăng nhập bằng Google");
        }
        if (!encoder.matches(req.matKhau(), tk.getMatKhauHash())) {
            throw ApiException.unauthorized("Email hoặc mật khẩu không đúng");
        }
        kiemTraKhoa(tk);
        return response(tk);
    }

    /** Đăng nhập Google: tìm theo sub -> theo email (liên kết) -> tạo mới */
    @Transactional
    public AuthResponse loginGoogle(GoogleLoginRequest req) {
        GoogleTokenService.GoogleUser g = googleTokenService.verify(req.credential());
        if (!g.emailVerified()) throw ApiException.unauthorized("Email Google chưa được xác minh");

        TaiKhoan tk = taiKhoanRepo.findByGoogleSub(g.sub())
                .or(() -> taiKhoanRepo.findByEmailIgnoreCase(g.email()))
                .orElse(null);
        if (tk == null) {
            tk = new TaiKhoan();
            tk.setEmail(g.email().toLowerCase());
            tk.setHoTen(g.name() != null ? g.name() : g.email());
            tk.setNhaCungCap("GOOGLE");
            tk.setVaiTro(role(VaiTro.KHACH_HANG));
        }
        tk.setGoogleSub(g.sub());
        if (tk.getAnhDaiDien() == null) tk.setAnhDaiDien(g.picture());
        taiKhoanRepo.save(tk);
        kiemTraKhoa(tk);
        if (khachHangRepo.findByTaiKhoan_TaiKhoanId(tk.getTaiKhoanId()).isEmpty()) taoHoSo(tk);
        return response(tk);
    }

    @Transactional(readOnly = true)
    public UserInfo me(Integer taiKhoanId) {
        return info(taiKhoanRepo.findById(taiKhoanId).orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại")));
    }

    @Transactional
    public UserInfo updateProfile(Integer taiKhoanId, UpdateProfileRequest req) {
        TaiKhoan tk = taiKhoanRepo.findById(taiKhoanId).orElseThrow();
        tk.setHoTen(req.hoTen().trim());
        KhachHang kh = khachHangRepo.findByTaiKhoan_TaiKhoanId(taiKhoanId).orElseGet(() -> taoHoSo(tk));
        kh.setHoTen(tk.getHoTen());
        kh.setSoDienThoai(req.soDienThoai());
        kh.setDiaChi(req.diaChi());
        return info(tk);
    }

    @Transactional
    public void changePassword(Integer taiKhoanId, ChangePasswordRequest req) {
        TaiKhoan tk = taiKhoanRepo.findById(taiKhoanId).orElseThrow();
        if (tk.getMatKhauHash() != null &&
                (req.matKhauCu() == null || !encoder.matches(req.matKhauCu(), tk.getMatKhauHash()))) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        tk.setMatKhauHash(encoder.encode(req.matKhauMoi()));
    }

    private KhachHang taoHoSo(TaiKhoan tk) {
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoTen(tk.getHoTen());
        kh.setEmail(tk.getEmail());
        return khachHangRepo.save(kh);
    }

    private void kiemTraKhoa(TaiKhoan tk) {
        if ("KHOA".equals(tk.getTrangThai())) throw ApiException.forbidden("Tài khoản đã bị khóa");
    }

    private VaiTro role(String ma) {
        return vaiTroRepo.findByMaVaiTro(ma).orElseThrow(() -> new IllegalStateException("Thiếu vai trò " + ma));
    }

    private AuthResponse response(TaiKhoan tk) {
        return new AuthResponse(jwtService.generate(tk), info(tk));
    }

    private UserInfo info(TaiKhoan tk) {
        KhachHang kh = khachHangRepo.findByTaiKhoan_TaiKhoanId(tk.getTaiKhoanId()).orElse(null);
        return new UserInfo(tk.getTaiKhoanId(), tk.getEmail(), tk.getHoTen(), tk.getAnhDaiDien(),
                tk.getVaiTro().getMaVaiTro(), tk.getNhaCungCap(),
                kh == null ? null : kh.getSoDienThoai(), kh == null ? null : kh.getDiaChi());
    }
}
