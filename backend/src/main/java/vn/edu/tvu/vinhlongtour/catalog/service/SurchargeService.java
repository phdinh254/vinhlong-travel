package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.catalog.repository.SurchargeRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.SurchargeResponse;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.util.List;

/**
 * Nghiệp vụ phụ thu: khách xem các khoản đang áp dụng, admin quản lý tất cả.
 */
@Service
@Transactional
public class SurchargeService {

    private final SurchargeRepository repo;

    public SurchargeService(SurchargeRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SurchargeResponse> listActive() {
        return repo.findByActiveTrueOrderByName().stream().map(SurchargeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SurchargeResponse> listAll() {
        return repo.findAll(Sort.by("name")).stream().map(SurchargeResponse::from).toList();
    }

    public SurchargeResponse create(SurchargeRequest r) {
        UniqueNames.ensureFree(repo, r.name(), null, "Phụ thu");
        return SurchargeResponse.from(repo.save(new Surcharge(r.name(), r.method(), r.unitPrice(), r.description())));
    }

    public SurchargeResponse update(Long id, SurchargeRequest r) {
        Surcharge surcharge = find(id);
        UniqueNames.ensureFree(repo, r.name(), id, "Phụ thu");
        surcharge.update(r.name(), r.method(), r.unitPrice(), r.description(), r.active() == null || r.active());
        return SurchargeResponse.from(surcharge);
    }

    public void delete(Long id) {
        repo.delete(find(id));
        repo.flush();
    }

    private Surcharge find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Phụ thu không tồn tại"));
    }
}
