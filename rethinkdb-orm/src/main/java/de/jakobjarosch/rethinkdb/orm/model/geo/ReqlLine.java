package de.jakobjarosch.rethinkdb.orm.model.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.rethinkdb.gen.exc.ReqlCompileError;
import com.rethinkdb.gen.proto.TermType;

import java.util.*;


public class ReqlLine extends ReqlGeo {

    private final List<ReqlPoint> coordinates;


    // Constructor and parsing used to deserialize from RethinkDB response or jackson map structure.
    @JsonCreator
    ReqlLine(Map<String, Object> data) {
        this(parsePoints(data));
    }

    public ReqlLine(ReqlPoint... coordinates) {
        this(Arrays.asList(coordinates));
    }

    public ReqlLine(List<ReqlPoint> coordinates) {
        super(TermType.LINE, null, null);

        if (coordinates.size() < 2) {
            throw new ReqlCompileError("Line must contain at least 2 coordinates");
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
        ReqlLine reQLLine = (ReqlLine) o;
        return Objects.equals(coordinates, reQLLine.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }
}
