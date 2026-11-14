package vn.edu.tvu.vinhlongtour.catalog.domain;

import vn.edu.tvu.vinhlongtour.common.ApiException;

/**
 * Cách tính một khoản phụ thu. Mỗi cách tính tự cài cách đếm số lượng (đa hình):
 * nơi dùng chỉ gọi {@link #quantity(int, Integer)} mà không cần if/else theo từng loại.
 */
public enum SurchargeMethod {

    /** Tính theo số hành khách, ví dụ bảo hiểm du lịch. */
    PER_PERSON {
        @Override
        public int quantity(int passengers, Integer requestedUnits) {
            return passengers;
        }
    },

    /** Tính một lần cho cả đơn, ví dụ phí xuất hóa đơn. */
    PER_BOOKING {
        @Override
        public int quantity(int passengers, Integer requestedUnits) {
            return 1;
        }
    },

    /** Khách tự chọn số lượng, ví dụ phòng đơn, xe đạp thuê. */
    PER_UNIT {
        @Override
        public int quantity(int passengers, Integer requestedUnits) {
            if (requestedUnits == null || requestedUnits < 1) {
                throw ApiException.badRequest("INVALID_SURCHARGE_QUANTITY", "Phụ thu này cần chọn số lượng từ 1 trở lên");
            }
            return requestedUnits;
        }
    };

    /**
     * Số lượng tính tiền cho một đơn.
     *
     * @param passengers     số hành khách của đơn
     * @param requestedUnits số lượng khách chọn (chỉ dùng cho {@link #PER_UNIT})
     */
    public abstract int quantity(int passengers, Integer requestedUnits);
}
