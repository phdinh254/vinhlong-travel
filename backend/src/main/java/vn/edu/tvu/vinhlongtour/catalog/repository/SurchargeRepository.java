package vn.edu.tvu.vinhlongtour.catalog.repository;

import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;

import java.util.List;

/**
 * Truy vấn bảng phụ thu ({@code surcharges}).
 */
public interface SurchargeRepository extends UniqueNameRepository<Surcharge> {
    List<Surcharge> findByActiveTrueOrderByName();
}
