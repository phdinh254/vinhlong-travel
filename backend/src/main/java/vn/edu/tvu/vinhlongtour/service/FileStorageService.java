package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.exception.ApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Lưu ảnh tour, ảnh tin tức, ảnh chứng từ thanh toán/hoàn tiền vào thư mục uploads */
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Set<String> EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final AppProperties props;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Chưa chọn tệp");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!EXT.contains(ext) || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw ApiException.badRequest("Chỉ chấp nhận ảnh jpg, png, webp, gif");
        }
        LocalDate d = LocalDate.now();
        String rel = d.getYear() + "/" + String.format("%02d", d.getMonthValue()) + "/" + UUID.randomUUID() + "." + ext;
        Path target = Path.of(props.uploadDir()).toAbsolutePath().resolve(rel).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được tệp", e);
        }
        return "/uploads/" + rel;
    }
}
