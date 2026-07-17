package com.example.PieJuega.util;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {
    }

    public static Double distanceKm(
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude
    ) {
        if (originLatitude == null || originLongitude == null
                || destinationLatitude == null || destinationLongitude == null) {
            return null;
        }

        double latitudeDelta = Math.toRadians(destinationLatitude - originLatitude);
        double longitudeDelta = Math.toRadians(destinationLongitude - originLongitude);
        double firstLatitude = Math.toRadians(originLatitude);
        double secondLatitude = Math.toRadians(destinationLatitude);
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double distance = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
        return Math.round(distance * 10.0) / 10.0;
    }
}
