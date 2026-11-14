package vn.edu.tvu.vinhlongtour.upload;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Phục vụ ảnh đã tải lên tại {@code /uploads/**} từ thư mục lưu ảnh (ai cũng xem được, như ảnh trên trang).
 */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    private final ImageStorage storage;

    public UploadWebConfig(ImageStorage storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Thư mục chưa tồn tại thì URI không có "/" cuối, Spring sẽ coi là file chứ không phải thư mục
        String location = storage.dir().toUri().toString();
        registry.addResourceHandler(ImageStorage.URL_PREFIX + "**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
