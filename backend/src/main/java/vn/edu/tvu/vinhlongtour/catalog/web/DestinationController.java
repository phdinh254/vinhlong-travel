package vn.edu.tvu.vinhlongtour.catalog.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.catalog.service.DestinationService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.DestinationRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.DestinationResponse;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

/**
 * API điểm đến: khách xem và lọc các điểm đang hiển thị; admin xem cả điểm đã ẩn và thêm/sửa/xóa.
 */
@RestController
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/destinations")
    public PageResponse<DestinationResponse> search(@RequestParam(required = false) Long regionId,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(regionId, categoryId, q, false, page, size);
    }

    @GetMapping("/api/v1/destinations/{id}")
    public DestinationResponse get(@PathVariable Long id) {
        return service.get(id, false);
    }

    @GetMapping("/api/v1/admin/destinations")
    public PageResponse<DestinationResponse> adminSearch(@RequestParam(required = false) Long regionId,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return service.search(regionId, categoryId, q, true, page, size);
    }

    @GetMapping("/api/v1/admin/destinations/{id}")
    public DestinationResponse adminGet(@PathVariable Long id) {
        return service.get(id, true);
    }

    @PostMapping("/api/v1/admin/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationResponse create(@Valid @RequestBody DestinationRequest r) {
        return service.create(r);
    }

    @PutMapping("/api/v1/admin/destinations/{id}")
    public DestinationResponse update(@PathVariable Long id, @Valid @RequestBody DestinationRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/api/v1/admin/destinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
