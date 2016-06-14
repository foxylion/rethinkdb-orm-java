import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.jakobjarosch.rethinkdb.orm.annotation.Index;
import de.jakobjarosch.rethinkdb.orm.annotation.PrimaryKey;
import de.jakobjarosch.rethinkdb.orm.annotation.RethinkDBModel;

@RethinkDBModel(
        tableName = "foobar",
        indices = {
                @Index(fields = {"test"}, geo = true),
                @Index(fields = {"field1", "field2"})
        }
)
/*@JsonIgnoreProperties(ignoreUnknown = true)*/
public class TestModel {

    @PrimaryKey
    private String id;

    private String test;

    private String field1;
    private String field2;

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

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }
}
