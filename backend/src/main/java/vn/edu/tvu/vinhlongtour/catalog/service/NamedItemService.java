package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.NamedItem;
import vn.edu.tvu.vinhlongtour.catalog.repository.UniqueNameRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.NamedItemResponse;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.util.List;
import java.util.function.Function;

/**
 * Nghiệp vụ dùng chung cho các danh mục chỉ có tên (khu vực, loại hình, dịch vụ): xem, thêm, đổi tên, xóa.
 * Một class generic, tạo ba bean cho ba loại (xem {@link CatalogConfig}), thay vì chép ba service gần giống nhau.
 *
 * @param <T> loại danh mục, là lớp con của {@link NamedItem}
 */
public class NamedItemService<T extends NamedItem> {

    private final String path;
    private final String label;
    private final UniqueNameRepository<T> repo;
    private final Function<String, T> factory;

    /**
     * @param path    đoạn đường dẫn API, ví dụ {@code regions}
     * @param label   tên hiển thị trong thông báo lỗi, ví dụ "Khu vực"
     * @param factory cách tạo bản ghi mới từ tên, ví dụ {@code Region::new}
     */
    public NamedItemService(String path, String label, UniqueNameRepository<T> repo, Function<String, T> factory) {
        this.path = path;
        this.label = label;
        this.repo = repo;
        this.factory = factory;
    }

    public String path() {
        return path;
    }

    @Transactional(readOnly = true)
    public List<NamedItemResponse> list() {
        return repo.findAll(Sort.by("name")).stream().map(NamedItemResponse::from).toList();
    }

    @Transactional
    public NamedItemResponse create(String name) {
        UniqueNames.ensureFree(repo, name, null, label);
        return NamedItemResponse.from(repo.save(factory.apply(name)));
    }

    @Transactional
    public NamedItemResponse rename(Long id, String name) {
        T item = find(id);
        UniqueNames.ensureFree(repo, name, id, label);
        item.rename(name);
        return NamedItemResponse.from(item);
    }

    /** Xóa; nếu đang được dữ liệu khác dùng thì CSDL chặn và API trả 409. */
    @Transactional
    public void delete(Long id) {
        repo.delete(find(id));
        repo.flush();
    }

    private T find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", label + " không tồn tại"));
    }
}
