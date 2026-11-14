package vn.edu.tvu.vinhlongtour.tour.domain;

import jakarta.persistence.Embeddable;

/**
 * Một ảnh của tour. {@code cover} = ảnh đại diện hiển thị ở danh sách tour.
 * Thứ tự ảnh do vị trí trong danh sách của {@link Tour} quyết định.
 */
@Embeddable
public record TourImage(String url, String caption, boolean cover) {
}
