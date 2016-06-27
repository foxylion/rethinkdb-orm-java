import de.jakobjarosch.rethinkdb.orm.model.geo.ReqlPoint;
import de.jakobjarosch.rethinkdb.pool.RethinkDBPool;
import de.jakobjarosch.rethinkdb.pool.RethinkDBPoolBuilder;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.time.ZonedDateTime;
import java.util.Optional;

public class EntryPoint {

    public static void main(String[] args) throws InterruptedException {
        final RethinkDBPool pool = new RethinkDBPoolBuilder().build();
        try {
            TestModelDAO dao = new TestModelDAO(pool);

            // Initialize the table (creates the table, and indices)
            dao.initTable();

            TestModel model = new TestModel("1");
            model.setSubModel(new SubModel());
            model.getSubModel().setLastUpdate(ZonedDateTime.now());

            // Create the model in the database
            dao.create(model);

            model.setLocation(new ReqlPoint(127.0, 10.0));

            // Update the model in the database
            dao.update(model);

            // Read the model from the database
            Optional<TestModel> dbModel = dao.read("1");

            // Delete the model from the database
            dbModel.ifPresent(m -> {
                dao.delete("1");
            });

            // Subscriptions are handled by RxJava
            // This sample subscribes to the change feed in an async mode
            Subscription subscription = dao.changes()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(change -> {
                        System.out.println(Thread.currentThread().toString() + ": " + change);
                    });

            // Listen for 10 seconds on changes.
            Thread.sleep(10000);

            // Stop getting updates.
            subscription.unsubscribe();

            Thread.sleep(1000);
        } finally {
            pool.shutdown();
        }
    }
}
