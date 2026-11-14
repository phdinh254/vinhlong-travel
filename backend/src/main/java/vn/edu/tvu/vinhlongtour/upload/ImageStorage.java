package vn.edu.tvu.vinhlongtour.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/**
 * Lưu ảnh admin tải lên vào thư mục {@code app.upload.dir}. Chỉ nhận JPEG, PNG, GIF, WebP, nhận diện bằng
 * vài byte đầu file chứ không tin đuôi file hay Content-Type trình duyệt gửi (đổi tên file .html thành .jpg
 * không qua được). Tên file do máy chủ đặt ngẫu nhiên, nên không ghi đè hay chen vào thư mục khác được.
 */
@Service
public class ImageStorage {

    public static final String URL_PREFIX = "/uploads/";

    private final Path dir;

    public ImageStorage(AppProperties props) {
        this.dir = props.upload().dir().toAbsolutePath().normalize();
    }

    public Path dir() {
        return dir;
    }

    /** @return đường dẫn công khai của ảnh, ví dụ {@code /uploads/3f2a...jpg} */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("FILE_REQUIRED", "Chưa chọn ảnh");
        }
        try {
            byte[] head;
            try (InputStream in = file.getInputStream()) {
                head = in.readNBytes(12);
            }
            String extension = imageExtension(head);
            if (extension == null) {
                throw ApiException.badRequest("NOT_AN_IMAGE", "Chỉ nhận ảnh JPEG, PNG, GIF hoặc WebP");
            }
            String name = UUID.randomUUID() + "." + extension;
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name));
            }
            return URL_PREFIX + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Không lưu được ảnh tải lên", e);
        }
    }

    /** Đuôi file theo "chữ ký" ở đầu file ảnh; không phải ảnh thì trả {@code null}. */
    static String imageExtension(byte[] h) {
        if (startsWith(h, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(h, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        if (startsWith(h, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        if (h.length >= 12 && startsWith(h, 'R', 'I', 'F', 'F')
                && "WEBP".equals(new String(Arrays.copyOfRange(h, 8, 12), StandardCharsets.US_ASCII))) {
            return "webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] h, int... signature) {
        if (h.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((h[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
