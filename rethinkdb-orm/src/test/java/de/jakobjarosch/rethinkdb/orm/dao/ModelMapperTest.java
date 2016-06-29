package de.jakobjarosch.rethinkdb.orm.dao;


import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlGeo;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlLine;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPoint;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPolygon;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelMapperTest {

    public static class TestModel {
        public String id;
        public ReqlGeo geo;
    }

    private ModelMapper modelMapper;

    @Before
    public void setup() {
        modelMapper = new ModelMapper();
    }

    @Test
    public void modelToMap_ReqlPointPreserved() {
        final TestModel model = new TestModel();
        model.id = "test";
        model.geo = new ReqlPoint(20.1, 30.0);

        Map result = modelMapper.map(model);

        assertThat(result.get("geo")).isEqualTo(model.geo);
    }

    @Test
    public void modelToMap_ReqlLinePreserved() {
        final TestModel model = new TestModel();
        model.id = "test";
        model.geo = new ReqlLine(new ReqlPoint(20.1, 30.0), new ReqlPoint(22.1, 33.0));

        Map result = modelMapper.map(model);

        assertThat(result.get("geo")).isEqualTo(model.geo);
    }

    @Test
    public void modelToMap_ReqlPolygonPreserved() {
        final TestModel model = new TestModel();
        model.id = "test";
        model.geo = new ReqlPolygon(new ReqlPoint(20.1, 30.0), new ReqlPoint(22.1, 33.0), new ReqlPoint(24.1, 35.0));

        Map result = modelMapper.map(model);

        assertThat(result.get("geo")).isEqualTo(model.geo);
    }
}