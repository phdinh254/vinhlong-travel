package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.PickupPoint;
import vn.edu.tvu.vinhlongtour.catalog.repository.PickupPointRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PickupPointRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PickupPointResponse;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.util.List;

/**
 * Nghiệp vụ điểm đón khách (chỉ admin quản lý).
 */
@Service
@Transactional
public class PickupPointService {

    private final PickupPointRepository repo;

    public PickupPointService(PickupPointRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<PickupPointResponse> list() {
        return repo.findAll(Sort.by("name")).stream().map(PickupPointResponse::from).toList();
    }

    public PickupPointResponse create(PickupPointRequest r) {
        UniqueNames.ensureFree(repo, r.name(), null, "Điểm đón");
        return PickupPointResponse.from(repo.save(new PickupPoint(r.name(), r.address())));
    }

    public PickupPointResponse update(Long id, PickupPointRequest r) {
        PickupPoint point = find(id);
        UniqueNames.ensureFree(repo, r.name(), id, "Điểm đón");
        point.update(r.name(), r.address(), r.active() == null || r.active());
        return PickupPointResponse.from(point);
    }

    public void delete(Long id) {
        repo.delete(find(id));
        repo.flush();
    }

    private PickupPoint find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Điểm đón không tồn tại"));
    }
}
