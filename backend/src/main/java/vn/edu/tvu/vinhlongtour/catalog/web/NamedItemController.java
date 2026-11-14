package vn.edu.tvu.vinhlongtour.catalog.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.catalog.service.NamedItemService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.NamedItemRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.NamedItemResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * API cho ba danh mục chỉ có tên: {@code regions} (khu vực), {@code tour-categories} (loại hình),
 * {@code amenities} (dịch vụ). Ai cũng xem được; chỉ admin thêm/sửa/xóa.
 */
@RestController
public class NamedItemController {

    private static final String KIND = "{kind:regions|tour-categories|amenities}";

    private final Map<String, NamedItemService<?>> services;

    public NamedItemController(List<NamedItemService<?>> services) {
        this.services = services.stream().collect(Collectors.toMap(NamedItemService::path, Function.identity()));
    }

    @GetMapping("/api/v1/" + KIND)
    public List<NamedItemResponse> list(@PathVariable String kind) {
        return services.get(kind).list();
    }

    @PostMapping("/api/v1/admin/" + KIND)
    @ResponseStatus(HttpStatus.CREATED)
    public NamedItemResponse create(@PathVariable String kind, @Valid @RequestBody NamedItemRequest r) {
        return services.get(kind).create(r.name());
    }

    @PutMapping("/api/v1/admin/" + KIND + "/{id}")
    public NamedItemResponse rename(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody NamedItemRequest r) {
        return services.get(kind).rename(id, r.name());
    }

    @DeleteMapping("/api/v1/admin/" + KIND + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String kind, @PathVariable Long id) {
        services.get(kind).delete(id);
    }
}
