package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Embeddable;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Tọa độ (vĩ độ, kinh độ) của một địa điểm. Bất biến.
 */
@Embeddable
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw ApiException.badRequest("INVALID_COORDINATES", "Tọa độ không hợp lệ");
        }
    }
}
