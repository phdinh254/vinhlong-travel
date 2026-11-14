package vn.edu.tvu.vinhlongtour.catalog.service;

import vn.edu.tvu.vinhlongtour.catalog.repository.UniqueNameRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Kiểm tra trùng tên dùng chung cho mọi danh mục. Trùng thì báo {@code NAME_TAKEN}
 * thay vì để CSDL báo lỗi ràng buộc.
 */
final class UniqueNames {

    private UniqueNames() {
    }

    /** @param excludeId mã của bản ghi đang sửa (null khi tạo mới) */
    static void ensureFree(UniqueNameRepository<?> repo, String name, Long excludeId, String label) {
        String trimmed = name.trim();
        boolean taken = excludeId == null ? repo.existsByNameIgnoreCase(trimmed) : repo.existsByNameIgnoreCaseAndIdNot(trimmed, excludeId);
        if (taken) {
            throw ApiException.conflict("NAME_TAKEN", label + " \"" + trimmed + "\" đã tồn tại");
        }
    }
}
