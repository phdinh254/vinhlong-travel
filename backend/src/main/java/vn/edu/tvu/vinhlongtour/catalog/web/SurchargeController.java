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
import vn.edu.tvu.vinhlongtour.catalog.service.SurchargeService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeResponse;

import java.util.List;

/**
 * API phụ thu: khách xem các khoản đang áp dụng; admin xem tất cả và thêm/sửa/xóa.
 */
@RestController
public class SurchargeController {

    private final SurchargeService service;

    public SurchargeController(SurchargeService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/surcharges")
    public List<SurchargeResponse> listActive() {
        return service.listActive();
    }

    @GetMapping("/api/v1/admin/surcharges")
    public List<SurchargeResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/v1/admin/surcharges")
    @ResponseStatus(HttpStatus.CREATED)
    public SurchargeResponse create(@Valid @RequestBody SurchargeRequest r) {
        return service.create(r);
    }

    @PutMapping("/api/v1/admin/surcharges/{id}")
    public SurchargeResponse update(@PathVariable Long id, @Valid @RequestBody SurchargeRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/api/v1/admin/surcharges/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
