package vn.edu.tvu.vinhlongtour.catalog.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.catalog.domain.CancellationPolicy;
import vn.edu.tvu.vinhlongtour.catalog.domain.PenaltyTier;
import vn.edu.tvu.vinhlongtour.catalog.repository.CancellationPolicyRepository;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyRequest;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.CancellationPolicyResponse;
import vn.edu.tvu.vinhlongtour.catalog.web.CatalogDtos.PenaltyTierDto;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Nghiệp vụ chính sách hủy có phiên bản: khách xem chính sách đang áp dụng; admin tạo chính sách mới,
 * chỉ sửa/xóa được chính sách chưa tới ngày hiệu lực.
 */
@Service
@Transactional
public class CancellationPolicyService {

    private final CancellationPolicyRepository repo;
    private final Clock clock;

    public CancellationPolicyService(CancellationPolicyRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CancellationPolicyResponse current() {
        return repo.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(LocalDate.now(clock))
                .map(CancellationPolicyResponse::from)
                .orElseThrow(() -> ApiException.notFound("NO_POLICY", "Chưa có chính sách hủy nào đang áp dụng"));
    }

    @Transactional(readOnly = true)
    public List<CancellationPolicyResponse> listAll() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "effectiveFrom")).stream().map(CancellationPolicyResponse::from).toList();
    }

    public CancellationPolicyResponse create(CancellationPolicyRequest r) {
        return CancellationPolicyResponse.from(repo.save(new CancellationPolicy(r.name(), r.effectiveFrom(), r.description(), tiers(r))));
    }

    public CancellationPolicyResponse update(Long id, CancellationPolicyRequest r) {
        CancellationPolicy policy = find(id);
        policy.update(r.name(), r.effectiveFrom(), r.description(), tiers(r), LocalDate.now(clock));
        return CancellationPolicyResponse.from(policy);
    }

    public void delete(Long id) {
        CancellationPolicy policy = find(id);
        policy.ensureNotEffective(LocalDate.now(clock));
        repo.delete(policy);
        repo.flush();
    }

    private static List<PenaltyTier> tiers(CancellationPolicyRequest r) {
        return r.tiers().stream().map(PenaltyTierDto::toTier).toList();
    }

    private CancellationPolicy find(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Chính sách hủy không tồn tại"));
    }
}
