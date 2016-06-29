package de.jakobjarosch.rethinkdb.orm.model.geo;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rethinkdb.gen.proto.TermType;

import java.util.Map;
import java.util.Objects;

public class ReqlPoint extends ReqlGeo {

    private final double longitude;
    private final double latitude;

    // Constructor and parsing used to deserialize from RethinkDB response or jackson map structure.
    @JsonCreator
    ReqlPoint(Map<String, Object> data) {
        this(parseLongitude(data),
                parseLatitude(data));
    }

    public ReqlPoint(double longitude, double latitude) {
        super(TermType.POINT, null, null);
        args.coerceAndAdd(longitude);
        args.coerceAndAdd(latitude);
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @JsonProperty()
    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @Override
    public String toString() {
        return "{ longitude: " + longitude + ", latitude: " + latitude + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqlPoint reQLPoint = (ReqlPoint) o;
        return Double.compare(reQLPoint.latitude, latitude) == 0 &&
                Double.compare(reQLPoint.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }
}
