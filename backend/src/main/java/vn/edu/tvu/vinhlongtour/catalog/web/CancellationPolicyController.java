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
import vn.edu.tvu.vinhlongtour.catalog.service.CancellationPolicyService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyResponse;

import java.util.List;

/**
 * API chính sách hủy: khách xem chính sách đang áp dụng (trang Chính sách);
 * admin xem các phiên bản, tạo mới, sửa/xóa phiên bản chưa có hiệu lực.
 */
@RestController
public class CancellationPolicyController {

    private final CancellationPolicyService service;

    public CancellationPolicyController(CancellationPolicyService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/cancellation-policies/current")
    public CancellationPolicyResponse current() {
        return service.current();
    }

    @GetMapping("/api/v1/admin/cancellation-policies")
    public List<CancellationPolicyResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/v1/admin/cancellation-policies")
    @ResponseStatus(HttpStatus.CREATED)
    public CancellationPolicyResponse create(@Valid @RequestBody CancellationPolicyRequest r) {
        return service.create(r);
    }

    @PutMapping("/api/v1/admin/cancellation-policies/{id}")
    public CancellationPolicyResponse update(@PathVariable Long id, @Valid @RequestBody CancellationPolicyRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/api/v1/admin/cancellation-policies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
