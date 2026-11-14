package vn.edu.tvu.vinhlongtour.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Đổi danh sách mã do client gửi lên thành các bản ghi; có mã không tồn tại thì báo lỗi 400 rõ ràng
 * thay vì bỏ qua lặng lẽ.
 */
public final class Lookups {

    private Lookups() {
    }

    public static <T> List<T> all(JpaRepository<T, Long> repo, Collection<Long> ids, String code, String message) {
        Set<Long> unique = new HashSet<>(ids);
        List<T> found = repo.findAllById(unique);
        if (found.size() != unique.size()) {
            throw ApiException.badRequest(code, message);
        }
        return found;
    }

    public static <T> T one(JpaRepository<T, Long> repo, Long id, String code, String message) {
        return repo.findById(id).orElseThrow(() -> ApiException.badRequest(code, message));
    }
}
