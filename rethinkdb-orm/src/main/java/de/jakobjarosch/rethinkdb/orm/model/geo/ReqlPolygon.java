package de.jakobjarosch.rethinkdb.orm.model.geo;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.rethinkdb.gen.exc.ReqlCompileError;
import com.rethinkdb.gen.proto.TermType;

import java.util.*;

public class ReqlPolygon extends ReqlGeo {

    private final List<ReqlPoint> coordinates;

    // Constructor and parsing used to deserialize from RethinkDB response or jackson map structure.
    @JsonCreator
    ReqlPolygon(Map<String, Object> data) {
        this(parsePoints(data));
    }

    public ReqlPolygon(ReqlPoint... coordinates) {
        this(Arrays.asList(coordinates));
    }

    public ReqlPolygon(List<ReqlPoint> coordinates) {
        super(TermType.POLYGON, null, null);

        if (coordinates.size() < 3) {
            throw new ReqlCompileError("Polygon must contain at least 3 coordinates");
        }

        coordinates.forEach(args::coerceAndAdd);

        this.coordinates = new ArrayList<>(coordinates);
    }

    public List<ReqlPoint> getCoordinates() {
        return Collections.unmodifiableList(coordinates);
    }

    @Override
    public String toString() {
        return "[ " + Joiner.on(", ").join(coordinates) + " ]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqlPolygon that = (ReqlPolygon) o;
        return Objects.equals(coordinates, that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }
}
