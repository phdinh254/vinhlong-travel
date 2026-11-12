package vn.edu.tvu.vinhlongtour.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.TourDtos.*;
import vn.edu.tvu.vinhlongtour.service.AdminTourService;
import vn.edu.tvu.vinhlongtour.service.TourService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTourController {
    private final TourService tourService;
    private final AdminTourService adminTourService;

    @GetMapping("/tours")
    public Page<TourCard> list(@RequestParam(required = false) String q, @RequestParam(required = false) String trangThai,
                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return tourService.search(q, null, null, null, null, (trangThai == null || trangThai.isBlank()) ? null : trangThai, page, size);
    }

    @GetMapping("/tours/{id}")
    public TourDetail get(@PathVariable Integer id) {
        return tourService.detailAdmin(id);
    }

    @PostMapping("/tours")
    public TourDetail create(@Valid @RequestBody TourSaveRequest r) {
        return adminTourService.saveTour(null, r);
    }

    @PutMapping("/tours/{id}")
    public TourDetail update(@PathVariable Integer id, @Valid @RequestBody TourSaveRequest r) {
        return adminTourService.saveTour(id, r);
    }

    @DeleteMapping("/tours/{id}")
    public void delete(@PathVariable Integer id) {
        adminTourService.deleteTour(id);
    }

    @PostMapping("/dots")
    public DotDto createDot(@Valid @RequestBody DotSaveRequest r) {
        return adminTourService.saveDot(null, r);
    }

    @PutMapping("/dots/{id}")
    public DotDto updateDot(@PathVariable Integer id, @Valid @RequestBody DotSaveRequest r) {
        return adminTourService.saveDot(id, r);
    }

    @DeleteMapping("/dots/{id}")
    public void deleteDot(@PathVariable Integer id) {
        adminTourService.deleteDot(id);
    }

    @GetMapping("/dots/{id}/lich-trinh")
    public List<NgayDto> lichTrinh(@PathVariable Integer id) {
        return tourService.lichTrinhCuaDot(id);
    }

    @PutMapping("/dots/{id}/lich-trinh")
    public List<NgayDto> saveLichTrinh(@PathVariable Integer id, @Valid @RequestBody List<NgayInput> ngays) {
        return adminTourService.saveLichTrinhDot(id, ngays);
    }
}
