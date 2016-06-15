import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import de.jakobjarosch.rethinkdb.orm.model.ChangeFeedElement;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class EntryPoint {

    public static void main(String[] args) throws InterruptedException {
        try (Connection connection = RethinkDB.r.connection().hostname("localhost").db("test").connect()) {
            TestModelDAO dao = new TestModelDAO(() -> connection);

            // Initialize the table (creates the table, and indices)
            dao.initTable();

            TestModel model = new TestModel("1");
            model.setSubModel(new SubModel());
            model.getSubModel().setMyField("asd");

            // Create the model in the database
            dao.create(model);

            model.setTest("foobar");

            // Update the model in the database
            dao.update(model);

            // Read the model from the database
            model = dao.read("1");
            dao.delete(model.getId());

            // Subscriptions are handled by RxJava
            // This sample subscribes to the change feed in an async mode
            final Observable<ChangeFeedElement<TestModel>> subscribed = dao.changes().subscribeOn(Schedulers.newThread());
            final Subscription subscription = subscribed.subscribe(change -> {
                System.out.println(Thread.currentThread().toString() + ": " + change);
            });

            // Listen for 10 seconds on changes.
            Thread.sleep(10000);

            // Stop getting updates.
            subscription.unsubscribe();

            Thread.sleep(1000);
        }
    }
}
