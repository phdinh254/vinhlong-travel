package vn.edu.tvu.vinhlongtour.catalog.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.catalog.service.PickupPointService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PickupPointRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PickupPointResponse;

import java.util.List;

/**
 * API quản lý điểm đón (chỉ admin). Khách sẽ thấy điểm đón qua từng đợt khởi hành (giai đoạn 3).
 */
@RestController
@RequestMapping("/api/v1/admin/pickup-points")
public class PickupPointController {

    private final PickupPointService service;

    public PickupPointController(PickupPointService service) {
        this.service = service;
    }

    @GetMapping
    public List<PickupPointResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PickupPointResponse create(@Valid @RequestBody PickupPointRequest r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    public PickupPointResponse update(@PathVariable Long id, @Valid @RequestBody PickupPointRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
