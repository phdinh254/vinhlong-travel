package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.AgeRange;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.repository.PassengerTypeRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PassengerTypeRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PassengerTypeResponse;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Nghiệp vụ loại khách. Quy tắc chính: khoảng tuổi các loại khách không được chồng nhau,
 * để mỗi hành khách chỉ thuộc đúng một loại khi tính giá.
 */
@Service
@Transactional
public class PassengerTypeService {

    private final PassengerTypeRepository repo;

    public PassengerTypeService(PassengerTypeRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<PassengerTypeResponse> list() {
        return repo.findAll().stream()
                .sorted(Comparator.comparingInt((PassengerType p) -> p.getAgeRange().minAge()).reversed())
                .map(PassengerTypeResponse::from).toList();
    }

    public PassengerTypeResponse create(PassengerTypeRequest r) {
        AgeRange range = new AgeRange(r.minAge(), r.maxAge());
        validate(r.name(), range, null);
        return PassengerTypeResponse.from(repo.save(new PassengerType(r.name(), range, r.seats())));
    }

    public PassengerTypeResponse update(Long id, PassengerTypeRequest r) {
        PassengerType type = find(id);
        AgeRange range = new AgeRange(r.minAge(), r.maxAge());
        validate(r.name(), range, id);
        type.update(r.name(), range, r.seats());
        return PassengerTypeResponse.from(type);
    }

    public void delete(Long id) {
        repo.delete(find(id));
        repo.flush();
    }

    private void validate(String name, AgeRange range, Long excludeId) {
        UniqueNames.ensureFree(repo, name, excludeId, "Loại khách");
        repo.findAll().stream()
                .filter(other -> !Objects.equals(other.getId(), excludeId))
                .filter(other -> other.getAgeRange().overlaps(range))
                .findFirst()
                .ifPresent(other -> {
                    throw ApiException.conflict("AGE_RANGE_OVERLAP", "Khoảng tuổi " + range.minAge() + "–" + range.maxAge()
                            + " chồng với loại khách \"" + other.getName() + "\"");
                });
    }

    private PassengerType find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Loại khách không tồn tại"));
    }
}
