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
import vn.edu.tvu.vinhlongtour.catalog.service.PassengerTypeService;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PassengerTypeRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PassengerTypeResponse;

import java.util.List;

/**
 * API loại khách: ai cũng xem được (form đặt tour cần), chỉ admin thêm/sửa/xóa.
 */
@RestController
public class PassengerTypeController {

    private final PassengerTypeService service;

    public PassengerTypeController(PassengerTypeService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/passenger-types")
    public List<PassengerTypeResponse> list() {
        return service.list();
    }

    @PostMapping("/api/v1/admin/passenger-types")
    @ResponseStatus(HttpStatus.CREATED)
    public PassengerTypeResponse create(@Valid @RequestBody PassengerTypeRequest r) {
        return service.create(r);
    }

    @PutMapping("/api/v1/admin/passenger-types/{id}")
    public PassengerTypeResponse update(@PathVariable Long id, @Valid @RequestBody PassengerTypeRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/api/v1/admin/passenger-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
