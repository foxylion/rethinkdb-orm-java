import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.jakobjarosch.rethinkdb.orm.annotation.Index;
import de.jakobjarosch.rethinkdb.orm.annotation.PrimaryKey;
import de.jakobjarosch.rethinkdb.orm.annotation.RethinkDBModel;
import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPoint;

@RethinkDBModel(
        tableName = "my_table",
        indices = {
                @Index(fields = {"location"}, geo = true),
                @Index(fields = {"field1", "field2"})
        }
)
public class TestModel {

    @PrimaryKey
    private String id;
    private ReqlPoint location;
    private SubModel subModel;

    TestModel() {
    }

    public TestModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public ReqlPoint getLocation() {
        return location;
    }

    public void setLocation(ReqlPoint location) {
        this.location = location;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }
}
