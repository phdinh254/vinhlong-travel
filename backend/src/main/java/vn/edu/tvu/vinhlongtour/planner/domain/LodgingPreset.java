package vn.edu.tvu.vinhlongtour.planner.domain;

import vn.edu.tvu.vinhlongtour.catalog.domain.GeoPoint;

/**
 * Các nơi lưu trú gợi ý sẵn để khách chọn nhanh (mỗi ngày đi và về đây). Tọa độ là trung tâm khu vực,
 * gần đúng; khách muốn chính xác thì gửi tọa độ tự chọn trên bản đồ.
 */
public enum LodgingPreset {

    VINH_LONG_CENTER("Trung tâm Vĩnh Long (cũ)", new GeoPoint(10.2537, 105.9722)),
    BEN_TRE_CENTER("Trung tâm Bến Tre (cũ)", new GeoPoint(10.2434, 106.3756)),
    TRA_VINH_CENTER("Trung tâm Trà Vinh (cũ)", new GeoPoint(9.9347, 106.3420)),
    AN_BINH_ISLAND("Cù lao An Bình", new GeoPoint(10.2938, 105.9821));

    private final String displayName;
    private final GeoPoint location;

    LodgingPreset(String displayName, GeoPoint location) {
        this.displayName = displayName;
        this.location = location;
    }

    public String displayName() {
        return displayName;
    }

    public GeoPoint location() {
        return location;
    }
}
