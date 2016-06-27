package de.jakobjarosch.rethinkdb.orm.model.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.rethinkdb.gen.exc.ReqlCompileError;
import com.rethinkdb.gen.proto.TermType;

import java.util.*;


public class ReqlLine extends ReqlGeo {

    private final List<ReqlPoint> points;

    @JsonCreator
    ReqlLine(Map<String, Object> data) {
        this(GeoFactory.parseLine(data));
    }

    public ReqlLine(ReqlPoint... points) {
        this(Arrays.asList(points));
    }

    public ReqlLine(List<ReqlPoint> points) {
        super(TermType.LINE, null, null);

        if (points.size() < 2) {
            throw new ReqlCompileError("Line must contain at least 2 points");
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
        ReqlLine reQLLine = (ReqlLine) o;
        return Objects.equals(points, reQLLine.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(points);
    }
}
