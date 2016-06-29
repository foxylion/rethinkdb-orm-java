package de.jakobjarosch.rethinkdb.orm.model.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rethinkdb.ast.ReqlAst;
import com.rethinkdb.gen.proto.TermType;
import com.rethinkdb.model.Arguments;
import com.rethinkdb.model.OptArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public abstract class ReqlGeo extends ReqlAst {

    public static final String MAPPING_CLASS_KEY = "__geoClassName";

    @JsonProperty(MAPPING_CLASS_KEY)
    private final String geoClassName;

    protected ReqlGeo(TermType termType, Arguments args, OptArgs optargs) {
        super(termType, args, optargs);
        geoClassName = getClass().getName();
    }

    static double parseLongitude(Map<String, Object> data) {
        if (data.containsKey("longitude"))
            return ReqlGeo.convertCoordinate(data.get("longitude"));
        else
            return ReqlGeo.convertCoordinate(((List) data.get("coordinates")).get(0));
    }

    static double parseLatitude(Map<String, Object> data) {
        if (data.containsKey("latitude"))
            return ReqlGeo.convertCoordinate(data.get("latitude"));
        else
            return ReqlGeo.convertCoordinate(((List) data.get("coordinates")).get(1));
    }

    static List<ReqlPoint> parsePoints(Map<String, Object> data) {
        List<?> points = (List<?>) data.get("coordinates");

        if (points.isEmpty()) {
            return new ArrayList<>();
        } else if (points.get(0) instanceof Map) {
            // Deserialize Jackson serialization
            return points.stream()
                    .map(p -> toPoint((Map) p))
                    .collect(Collectors.toList());
        } else if (points.get(0) instanceof List) {
            // Deserialize ReQL result
            if(((List) points.get(0)).get(0) instanceof List) {
                // The data structure used by polygon has one more cascaded list
                // points[][] = array(longitude, latitude)
                points = (List) points.get(0);
            }
            return points.stream()
                    .map(p -> parseReqlCoordinate((List) p))
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Can't handle" + points.get(0).getClass() + " as coordinate data type.");
        }
    }

    @SuppressWarnings("unchecked")
    private static ReqlPoint toPoint(Map map) {
        return new ReqlPoint(map);
    }

    private static ReqlPoint parseReqlCoordinate(List<?> data) {
        return new ReqlPoint(convertCoordinate(data.get(0)), convertCoordinate(data.get(1)));
    }

    private static double convertCoordinate(Object longitude) {
        if (longitude instanceof Long) {
            return (long) longitude;
        } else {
            return (double) longitude;
        }
    }
}
