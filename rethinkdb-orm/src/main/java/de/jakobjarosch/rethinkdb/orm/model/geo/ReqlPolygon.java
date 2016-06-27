package de.jakobjarosch.rethinkdb.orm.model.geo;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.rethinkdb.gen.exc.ReqlCompileError;
import com.rethinkdb.gen.proto.TermType;

import java.util.*;

public class ReqlPolygon extends ReqlGeo {

    private final List<ReqlPoint> points;

    @JsonCreator
    ReqlPolygon(Map<String, Object> data) {
        this(GeoFactory.parsePolygon(data));
    }

    public ReqlPolygon(ReqlPoint... points) {
        this(Arrays.asList(points));
    }

    public ReqlPolygon(List<ReqlPoint> points) {
        super(TermType.POLYGON, null, null);

        if (points.size() < 3) {
            throw new ReqlCompileError("Polygon must contain at least 3 points");
        }

        points.forEach(args::coerceAndAdd);

        this.points = new ArrayList<>(points);
    }

    public List<ReqlPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    @Override
    public String toString() {
        return "[ " + Joiner.on(", ").join(points) + " ]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqlPolygon that = (ReqlPolygon) o;
        return Objects.equals(points, that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points);
    }
}
