package vn.edu.tvu.vinhlongtour.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình bảo mật: API không lưu phiên (stateless), xác thực bằng token JWT ở header
 * {@code Authorization: Bearer ...}. Mặc định mọi API đều cần đăng nhập, trừ các đường dẫn
 * công khai liệt kê dưới đây; {@code /api/v1/admin/**} chỉ dành cho ADMIN.
 */
@Configuration
public class SecurityConfig {

    /** API chỉ đọc mà khách chưa đăng nhập cũng xem được (danh mục, điểm đến, chính sách). */
    private static final String[] PUBLIC_GET = {
            "/api/v1/regions", "/api/v1/tour-categories", "/api/v1/amenities", "/api/v1/passenger-types",
            "/api/v1/surcharges", "/api/v1/cancellation-policies/current", "/api/v1/destinations", "/api/v1/destinations/*",
            "/api/v1/tours", "/api/v1/tours/*", "/api/v1/tours/*/departures", "/api/v1/departures/*", "/api/v1/departures/*/itinerary",
            "/api/v1/bookings/lookup", "/api/v1/payments/vnpay/return", "/api/v1/payments/vnpay/ipn",
            "/api/v1/itineraries/lodging-presets", "/api/v1/articles", "/api/v1/articles/*", "/api/v1/site-settings", "/uploads/**"};

    /** API ghi mà khách vãng lai cũng gọi được, không cần biết ai gọi. */
    private static final String[] PUBLIC_POST = {"/api/v1/bookings/quote", "/api/v1/itineraries/suggest", "/api/v1/contact-messages"};

    /** API ghi khách vãng lai gọi được; nếu có token hợp lệ thì biết là khách nào (đơn gắn với tài khoản). */
    private static final String[] OPTIONAL_LOGIN_POST = {"/api/v1/bookings", "/api/v1/payments/vnpay", "/api/v1/bookings/*/cancellation-requests"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountJwtConverter jwtConverter,
                                            SecurityErrorHandler errors, AppProperties props) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Không dùng cookie đăng nhập nên không bị tấn công CSRF
                .cors(cors -> cors.configurationSource(corsSource(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.POST, OPTIONAL_LOGIN_POST).permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(ignoreTokenOnPublicApi())
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Frontend thường gắn token đã lưu vào mọi request. Nếu token đó hết hạn mà vẫn bị kiểm tra
     * ở API đăng nhập hay API công khai thì người dùng không đăng nhập lại được, không xem được trang,
     * nên bỏ qua token ở các API này.
     */
    private static BearerTokenResolver ignoreTokenOnPublicApi() {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> publicApi = new ArrayList<>();
        publicApi.add(paths.matcher("/api/v1/auth/**"));
        for (String pattern : PUBLIC_GET) {
            publicApi.add(paths.matcher(HttpMethod.GET, pattern));
        }
        for (String pattern : PUBLIC_POST) {
            publicApi.add(paths.matcher(HttpMethod.POST, pattern));
        }
        RequestMatcher matcher = new OrRequestMatcher(publicApi);
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> matcher.matches(request) ? null : resolver.resolve(request);
    }

    private static UrlBasedCorsConfigurationSource corsSource(AppProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.cors().allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
