import de.jakobjarosch.rethinkdb.orm.annotation.Index;
import de.jakobjarosch.rethinkdb.orm.annotation.PrimaryKey;
import de.jakobjarosch.rethinkdb.orm.annotation.RethinkDBModel;

@RethinkDBModel(
        tableName = "my_table",
        indices = {
                @Index(fields = {"test"}, geo = true),
                @Index(fields = {"field1", "field2"})
        }
)
public class TestModel {

    @PrimaryKey
    private String id;
    private String test;
    private SubModel subModel;

    TestModel() {
    }

    public TestModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }
}
