package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Embeddable;
import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Khoảng tuổi [minAge, maxAge] (tính cả hai đầu) của một loại khách.
 * Bất biến: tạo ra đã hợp lệ, không sửa được; muốn đổi thì tạo khoảng mới.
 */
@Embeddable
public record AgeRange(int minAge, int maxAge) {

    public AgeRange {
        if (minAge < 0 || minAge > maxAge) {
            throw ApiException.badRequest("INVALID_AGE_RANGE", "Khoảng tuổi không hợp lệ: từ " + minAge + " đến " + maxAge);
        }
    }

    public boolean contains(int age) {
        return age >= minAge && age <= maxAge;
    }

    /** Hai khoảng chồng nhau khi có ít nhất một độ tuổi thuộc cả hai. */
    public boolean overlaps(AgeRange other) {
        return minAge <= other.maxAge && other.minAge <= maxAge;
    }
}
