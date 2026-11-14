package vn.edu.tvu.vinhlongtour.planner.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.planner.domain.LodgingPreset;
import vn.edu.tvu.vinhlongtour.planner.service.TripPlannerService;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.LodgingOption;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SaveTripRequest;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SavedTripDetail;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SavedTripSummary;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.SuggestRequest;
import vn.edu.tvu.vinhlongtour.planner.web.TripDtos.TripPlan;

import java.util.Arrays;
import java.util.List;

/**
 * API gợi ý lịch trình (ai cũng dùng được) và lịch trình đã lưu (cần đăng nhập, chỉ thấy lịch của mình).
 */
@RestController
public class TripController {

    private final TripPlannerService service;

    public TripController(TripPlannerService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/itineraries/lodging-presets")
    public List<LodgingOption> lodgingPresets() {
        return Arrays.stream(LodgingPreset.values())
                .map(p -> new LodgingOption(p, p.displayName(), p.location().latitude(), p.location().longitude()))
                .toList();
    }

    @PostMapping("/api/v1/itineraries/suggest")
    public TripPlan suggest(@Valid @RequestBody SuggestRequest r) {
        return service.suggest(r);
    }

    /** Lưu lịch trình: máy chủ tự gợi ý lại từ tham số khách gửi, không nhận lịch do client tự dựng. */
    @PostMapping("/api/v1/itineraries")
    @ResponseStatus(HttpStatus.CREATED)
    public SavedTripDetail save(@Valid @RequestBody SaveTripRequest r, Authentication auth) {
        return service.save(r, Long.valueOf(auth.getName()));
    }

    @GetMapping("/api/v1/me/itineraries")
    public List<SavedTripSummary> mine(Authentication auth) {
        return service.listMine(Long.valueOf(auth.getName()));
    }

    @GetMapping("/api/v1/me/itineraries/{id}")
    public SavedTripDetail mine(Authentication auth, @PathVariable Long id) {
        return service.getMine(Long.valueOf(auth.getName()), id);
    }

    @DeleteMapping("/api/v1/me/itineraries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.deleteMine(Long.valueOf(auth.getName()), id);
    }
}
