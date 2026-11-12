package vn.edu.tvu.vinhlongtour.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.entity.TaiKhoan;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties props) {
        byte[] bytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài ít nhất 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = props.jwt().expirationMinutes() * 60_000L;
    }

    public String generate(TaiKhoan tk) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(tk.getTaiKhoanId()))
                .claim("email", tk.getEmail())
                .claim("role", tk.getVaiTro().getMaVaiTro())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthUser(Integer.valueOf(c.getSubject()), c.get("email", String.class), c.get("role", String.class));
    }
}
