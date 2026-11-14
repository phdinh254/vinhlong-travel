package vn.edu.tvu.vinhlongtour.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Thông tin hiển thị trên Swagger UI, và nút "Authorize" để dán token đăng nhập
 * khi thử các API cần đăng nhập.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "VinhLong Tour API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
