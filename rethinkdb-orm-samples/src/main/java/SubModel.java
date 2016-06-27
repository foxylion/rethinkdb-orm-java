import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class SubModel {

    private ZonedDateTime lastUpdate;

    public ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
