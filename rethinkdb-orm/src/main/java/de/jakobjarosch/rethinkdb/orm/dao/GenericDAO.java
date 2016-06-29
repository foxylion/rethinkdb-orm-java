package de.jakobjarosch.rethinkdb.orm.dao;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.GetField;
import com.rethinkdb.gen.ast.IndexCreate;
import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.gen.exc.ReqlClientError;
import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.gen.exc.ReqlInternalError;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import de.jakobjarosch.rethinkdb.orm.model.ChangeFeedElement;
import de.jakobjarosch.rethinkdb.orm.model.IndexModel;
import rx.Observable;

import javax.inject.Provider;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenericDAO<T, PK> {

    private static final RethinkDB R = RethinkDB.r;
    private static final ModelMapper MAPPER = new ModelMapper();


    private final Provider<Connection> connectionProvider;
    private final Class<T> clazz;
    private final String tableName;
    private final String primaryKey;

    private final Set<IndexModel> indices = new HashSet<>();

    public GenericDAO(Provider<Connection> connection, Class<T> clazz, String tableName, String primaryKey) {
        this.connectionProvider = connection;
        this.clazz = clazz;
        this.tableName = tableName;
        this.primaryKey = primaryKey;
    }

    protected void addIndex(boolean geo, String fields) {
        this.indices.add(new IndexModel(geo, fields.split(",")));
    }

    /**
     * Initialize the table, automatically create the table and indices if they do not yet exist.
     */
    public void initTable() {
        try (Connection connection = connectionProvider.get()) {
            if (!hasTable(connection, tableName)) {
                R.tableCreate(tableName).optArg("primary_key", primaryKey).run(connection);
            }

            for (IndexModel index : indices) {
                String indexName = Joiner.on("_").join(index.getFields());
                if (!hasIndex(connection, indexName)) {
                    IndexCreate indexCreate = R.table(tableName)
                            .indexCreate(indexName, row -> indexFieldsToReQL(row, index.getFields()));
                    if (index.isGeo()) {
                        indexCreate = indexCreate.optArg("geo", true);
                    }

                    indexCreate.run(connection);
                }
            }
        }
    }

    /**
     * Creates a model in the RethinkDB table.
     *
     * @param model Model which should be created.
     * @throws ReqlClientError Error is thrown when there was an error.
     *                         E.g. there was already a model with the same primary key.
     */
    public void create(T model) {
        try (Connection connection = connectionProvider.get()) {
            Map<?, ?> map = MAPPER.map(model);
            Map<String, ?> result = R.table(tableName).insert(map).run(connection);

            if (((Long) result.get("errors")) > 0) {
                throw new ReqlClientError("Failed to create model: %s", ((String) result.get("first_error")).split("\n")[0]);
            }
        }
    }

    /**
     * Retrieves a model with the given primary key.
     *
     * @param id The primary key of the model which should be retrieved.
     * @return Maybe the model matching the given primary key.
     */
    public Optional<T> read(PK id) {
        try (Connection connection = connectionProvider.get()) {
            Map<?, ?> map = R.table(tableName).get(id).run(connection);
            return Optional.ofNullable(MAPPER.map(map, clazz));
        }
    }

    /**
     * Returns all models in the table, use this with caution,
     * without filtering it could return a huge amount of data.
     *
     * @return Returns a list of all models in the table.
     */
    public DAOIterator<T> read() {
        return read(t -> t);
    }

    /**
     * Retrieves a iterator returning all models matching the given filter.
     * <br>
     * Be sure to call {@link DAOIterator#close()} after finishing the iterator.
     *
     * @param filter The filter function which should be applied when executing the query.
     * @return An iterator for models matching the given filter.
     */
    public DAOIterator<T> read(Function<Table, ReqlExpr> filter) {
        try (Connection connection = connectionProvider.get()) {
            final Table table = R.table(tableName);
            Object result = filter.apply(table).run(connection);
            if (result instanceof List) {
                List<T> list = ((List<?>) result).stream()
                        .map(map -> MAPPER.map((Map) map, clazz))
                        .collect(Collectors.toList());
                return new DAOIterator<>(list.iterator(), clazz, MAPPER);
            } else if (result instanceof Map) {
                return new DAOIterator<>(Lists.newArrayList(result).iterator(), clazz, MAPPER);
            } else if (result instanceof Cursor) {
                Cursor<?> cursor = (Cursor<?>) result;
                return new DAOIterator<>(cursor, clazz, MAPPER);
            } else {
                throw new ReqlInternalError("Unknown return type for query: " + result.getClass());
            }
        }
    }

    /**
     * Updates a model.
     *
     * @param id    The id of the model which should be updated.
     * @param model The model which should be updated.
     */
    public void update(PK id, T model) {
        try (Connection connection = connectionProvider.get()) {
            Map<?, ?> map = MAPPER.map(model);
            Map<String, ?> result = R.table(tableName).get(id).update(map).run(connection);

            if (((Long) result.get("errors")) > 0) {
                throw new ReqlClientError("Failed to update model. %s", ((String) result.get("first_error")).split("\n")[0]);
            }
        }
    }

    /**
     * Updates a model in the non atomic way. See <a href="https://rethinkdb.com/api/java/update/">ReQL command: update</a>
     * for more details.
     *
     * @param id    The id of the model which should be updated.
     * @param model The model which should be updated.
     */
    public void updateNonAtomic(PK id, T model) {
        try (Connection connection = connectionProvider.get()) {
            R.table(tableName).get(id).update(model).run(connection, OptArgs.of("non_atomic", true));
        }
    }

    /**
     * Deletes a model from the table.
     *
     * @param id The primary key of the model which should be removed.
     */
    public void delete(PK id) {
        try (Connection connection = connectionProvider.get()) {
            R.table(tableName).get(id).delete().run(connection);
        }
    }

    /**
     * Provides a change feed of all changes which occur after subscribing to the returned {@link Observable}.
     *
     * @return Returns an {@link Observable} which subscribes to all changes made after the subscription started.
     */
    public Observable<ChangeFeedElement<T>> changes() {
        return changes(t -> t);
    }

    /**
     * Provides a change feed of all matching changes which occur after subscribing to the returned {@link Observable}.
     *
     * @param filter A filter for the change feed to only show changes matching the filter.
     * @return Returns an {@link Observable} which subscribes to all matching changes made after the subscription started.
     */
    @SuppressWarnings("unchecked")
    public Observable<ChangeFeedElement<T>> changes(Function<Table, ReqlExpr> filter) {
        return Observable.create(subscriber -> {
            Cursor<Map<?, Map<?, ?>>> cursor = null;
            try (Connection connection = connectionProvider.get()) {
                final Table table = R.table(tableName);
                cursor = filter.apply(table).changes().run(connection);

                while (!subscriber.isUnsubscribed()) {
                    Map<?, Map<?, ?>> map = cursor.next();
                    ChangeFeedElement<T> change = mapChangeFeedElement(map);
                    subscriber.onNext(change);
                }
            } catch (ReqlDriverError e) {
                if (e.getCause() instanceof InterruptedException) {
                    // We were interrupted because the subscription has been canceled.
                    // So we won't do anything.
                } else {
                    subscriber.onError(e);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }

    private boolean hasTable(Connection connection, String table) {
        List<String> tables = R.tableList().run(connection);
        return tables.contains(table);
    }

    private boolean hasIndex(Connection connection, String indexName) {
        List<String> indices = R.table(tableName).indexList().run(connection);
        return indices.contains(indexName);
    }

    private List indexFieldsToReQL(ReqlExpr row, String[] fields) {
        final GetField[] reQLFields = Arrays.stream(fields).map(f -> row.g(f)).collect(Collectors.toList()).toArray(new GetField[0]);
        return R.array(reQLFields);
    }

    private ChangeFeedElement<T> mapChangeFeedElement(Map<?, Map<?, ?>> map) {
        final Map<?, ?> oldVal = map.get("old_val");
        final Map<?, ?> newVal = map.get("new_val");

        final T oldValObj = oldVal != null ? MAPPER.map(oldVal, clazz) : null;
        final T newValObj = newVal != null ? MAPPER.map(newVal, clazz) : null;

        return new ChangeFeedElement<>(oldValObj, newValObj);
    }
}
