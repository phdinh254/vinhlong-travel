package vn.edu.tvu.vinhlongtour.catalog.domain;

/**
 * Độ tin cậy của tọa độ điểm đến (xem docs/data/README.md).
 */
public enum CoordinateQuality {
    /** Đúng vị trí điểm trên OpenStreetMap. */
    OSM,
    /** Tọa độ ghi trong bài Wikipedia. */
    WIKI,
    /** Lấy theo tâm xã/ấp hoặc điểm lân cận, có thể lệch vài km. */
    APPROXIMATE
}
