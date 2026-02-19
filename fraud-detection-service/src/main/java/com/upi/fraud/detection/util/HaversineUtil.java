package com.upi.fraud.detection.util;

/**
 * Haversine formula for calculating great-circle distance between two coordinates.
 * Used for Impossible Travel detection (Velocity / ATO fraud).
 */
public final class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {}

    /**
     * Returns distance in kilometres between two points (WGS84).
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Minimum speed (km/h) that would be required to travel the given distance
     * in the given time. timeElapsedSeconds must be positive.
     */
    public static double requiredSpeedKmh(double distanceKm, long timeElapsedSeconds) {
        if (timeElapsedSeconds <= 0) return 0;
        double hours = timeElapsedSeconds / 3600.0;
        return distanceKm / hours;
    }
}
