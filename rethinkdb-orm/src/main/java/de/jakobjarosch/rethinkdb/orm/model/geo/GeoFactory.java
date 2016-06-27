package de.jakobjarosch.rethinkdb.orm.model.geo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GeoFactory {

    @SuppressWarnings("unchecked")
    static List<ReqlPoint> parsePolygon(Map<String, Object> data) {
        List<List<List<?>>> points = (List<List<List<?>>>) data.get("coordinates");
        return points.get(0).stream().map(GeoFactory::parseCoordinatePair).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    static List<ReqlPoint> parseLine(Map<String, Object> data) {
        List<List<?>> points = (List<List<?>>) data.get("coordinates");
        return points.stream().map(GeoFactory::parseCoordinatePair).collect(Collectors.toList());
    }

    static double parseLongitude(Map<String, Object> data) {
        Object longitude = ((List) data.get("coordinates")).get(0);
        return convertCoordinate(longitude);
    }

    static double parseLatitude(Map<String, Object> data) {
        Object latitude = ((List) data.get("coordinates")).get(1);
        return convertCoordinate(latitude);
    }

    private static ReqlPoint parseCoordinatePair(List<?> data) {
        return new ReqlPoint(convertCoordinate(data.get(0)), convertCoordinate(data.get(1)));
    }

    private static double convertCoordinate(Object longitude) {
        if(longitude instanceof Long) {
            return (long) longitude;
        } else {
            return (double) longitude;
        }
    }
}
