package vn.edu.tvu.vinhlongtour.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API admin tải ảnh lên (ảnh tour, ảnh bìa bài viết, ảnh chứng từ chuyển khoản). Gửi dạng multipart, trường {@code file}.
 * Đường dẫn trả về dùng thẳng cho các trường ảnh khác (chúng chỉ nhận http(s):// hoặc /uploads/...).
 */
@RestController
public class UploadController {

    private final ImageStorage storage;

    public UploadController(ImageStorage storage) {
        this.storage = storage;
    }

    public record UploadResponse(String url) {}

    @PostMapping(value = "/api/v1/admin/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestPart("file") MultipartFile file) {
        return new UploadResponse(storage.store(file));
    }
}
