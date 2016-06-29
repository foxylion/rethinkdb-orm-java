package de.jakobjarosch.rethinkdb.orm.dao;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import de.jakobjarosch.rethinkdb.orm.model.ChangeFeedElement;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlLine;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPoint;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPolygon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericDAOTest {

    static class TestModel {
        static class SubModel {
            public String name;
        }

        TestModel() {
        }

        TestModel(String id) {
            this.id = id;
        }

        public String id;
        public Integer rank;
        public ReqlPoint point;
        public ReqlLine line;
        public ReqlPolygon polygon;
        public SubModel sub;
    }

    private static RethinkDB r = RethinkDB.r;
    private GenericDAO<TestModel, String> dao;

    @Before
    public void setup() {
        dao = new GenericDAO<>(() -> createConnection(), TestModel.class, "integration_test", "id");
        dao.addIndex(true, "point");
        dao.addIndex(true, "line");
        dao.addIndex(true, "polygon");
        dao.initTable();
    }

    @After
    public void teardown() {
        r.tableDrop("integration_test").run(createConnection());
    }

    @Test
    public void create() {
        TestModel model = new TestModel("1");
        dao.create(model);

        assertThat(dao.read("1")).isPresent();
    }

    @Test
    public void update() {
        TestModel model = new TestModel("1");
        dao.create(model);

        model.sub = new TestModel.SubModel();
        model.sub.name = "test";
        dao.update(model.id, model);

        assertThat(dao.read("1").get().sub.name).isEqualTo(model.sub.name);
    }

    @Test
    public void delete() {
        TestModel model = new TestModel("1");
        dao.create(model);

        dao.delete("1");

        assertThat(dao.read("1")).isNotPresent();
    }

    @Test
    public void read_geoModelCorrectlyReturned() {
        TestModel model = new TestModel("1");
        model.point = new ReqlPoint(10, 20.1);
        model.line = new ReqlLine(new ReqlPoint(1, 2), new ReqlPoint(3, 4));
        model.polygon = new ReqlPolygon(new ReqlPoint(5, 6), new ReqlPoint(7, 8), new ReqlPoint(9, 10), new ReqlPoint(5, 6));
        dao.create(model);

        TestModel dbModel = dao.read("1").get();

        assertThat(dbModel.id).isEqualTo(model.id);
        assertThat(dbModel.point).isEqualTo(model.point);
        assertThat(dbModel.line).isEqualTo(model.line);
        assertThat(dbModel.polygon).isEqualTo(model.polygon);
    }

    @Test
    public void read_returnsAll() {
        dao.create(new TestModel("1"));
        dao.create(new TestModel("2"));

        Iterator<TestModel> iterator = dao.read();
        List<TestModel> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);

        assertThat(result).hasSize(2);
    }

    @Test
    public void read_filter() {
        TestModel model1 = new TestModel("1");
        model1.rank = 10;
        dao.create(model1);

        TestModel model2 = new TestModel("2");
        model2.rank = 20;
        dao.create(model2);

        Iterator<TestModel> iterator = dao.read(t -> t.filter(r -> r.g("rank").gt(10)));
        List<TestModel> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo(model2.id);
    }

    @Test
    public void changes_filtered() throws InterruptedException {
        List<ChangeFeedElement<TestModel>> changes = new ArrayList<>();
        Subscription subscription = dao.changes(t -> t.filter(r -> r.g("rank").gt(10)))
                .subscribeOn(Schedulers.newThread())
                .subscribe(changes::add);

        dao.create(new TestModel("1"));
        final TestModel model2 = new TestModel("2");
        model2.rank = 20;
        dao.create(model2);
        model2.rank = 25;
        dao.update(model2.id, model2);
        dao.delete("2");

        Thread.sleep(500);

        subscription.unsubscribe();

        // Check if create, update and delete was received.
        assertThat(changes).hasSize(3);

        assertThat(changes.get(0).getOldValue()).isNotPresent();
        assertThat(changes.get(0).getNewValue()).isPresent();
        assertThat(changes.get(0).getNewValue().get().id).isEqualTo("2");

        assertThat(changes.get(1).getOldValue()).isPresent();
        assertThat(changes.get(1).getNewValue()).isPresent();
        assertThat(changes.get(1).getOldValue().get().rank).isEqualTo(20);
        assertThat(changes.get(1).getNewValue().get().rank).isEqualTo(25);

        assertThat(changes.get(2).getOldValue()).isPresent();
        assertThat(changes.get(2).getNewValue()).isNotPresent();
        assertThat(changes.get(2).getOldValue().get().id).isEqualTo("2");
    }

    private static Connection createConnection() {
        return r.connection().hostname("127.0.0.1").user("admin", "").db("test").connect();
    }
}