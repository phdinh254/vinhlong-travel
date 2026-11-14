package vn.edu.tvu.vinhlongtour.planner.service;

import vn.edu.tvu.vinhlongtour.catalog.domain.GeoPoint;

/**
 * Ước tính thời gian đi đường giữa hai tọa độ: khoảng cách đường chim bay (công thức Haversine)
 * × 1,3 cho đường vòng, đi trung bình 30 km/h.
 * shortcut: chưa tính phà, kẹt xe, đường nhỏ; khi cần chính xác hơn thì cho admin nhập bảng thời gian giữa các điểm.
 */
final class TravelTimes {

    static final double ROAD_FACTOR = 1.3;
    static final double SPEED_KMH = 30;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private TravelTimes() {
    }

    static double distanceKm(GeoPoint a, GeoPoint b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
    }

    static int minutes(GeoPoint a, GeoPoint b) {
        return (int) Math.ceil(distanceKm(a, b) * ROAD_FACTOR / SPEED_KMH * 60);
    }
}
