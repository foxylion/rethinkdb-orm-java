package de.jakobjarosch.rethinkdb.orm.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.GetField;
import com.rethinkdb.gen.ast.IndexCreate;
import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.gen.ast.Table;
import com.rethinkdb.gen.exc.ReqlDriverError;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    public void addIndex(boolean geo, String fields) {
        this.indices.add(new IndexModel(geo, fields.split(",")));
    }

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

    public void create(T model) {
        try (Connection connection = connectionProvider.get()) {
            Map<?, ?> map = MAPPER.convertValue(model, Map.class);
            R.table(tableName).insert(map).run(connection);
        }
    }

    public List<T> read() {
        try (Connection connection = connectionProvider.get()) {
            List<Map<?, ?>> mapList = R.table(tableName).run(connection);
            return mapList.stream()
                    .map(map -> MAPPER.convertValue(map, clazz))
                    .collect(Collectors.toList());
        }
    }

    public T read(PK id) {
        try (Connection connection = connectionProvider.get()) {
            Map<?, ?> map = R.table(tableName).get(id).run(connection);
            return MAPPER.convertValue(map, clazz);
        }
    }

    public List<T> read(Function<Table, ReqlExpr> filter) {
        try (Connection connection = connectionProvider.get()) {
            final Table table = R.table(tableName);
            return filter.apply(table).run(connection);
        }
    }

    public void update(T model) {
        try (Connection connection = connectionProvider.get()) {
            R.table(tableName).update(model).run(connection);
        }
    }

    public void delete(PK id) {
        try (Connection connection = connectionProvider.get()) {
            R.table(tableName).get(id).delete().run(connection);
        }
    }

    public Observable<ChangeFeedElement<T>> changes() {
        return changes(Optional.empty());
    }

    public Observable<ChangeFeedElement<T>> changes(Function<Table, ReqlExpr> filter) {
        return changes(Optional.of(filter));
    }

    @SuppressWarnings("unchecked")
    private Observable<ChangeFeedElement<T>> changes(Optional<Function<Table, ReqlExpr>> filter) {
        return Observable.create(subscriber -> {
            Cursor<Map<?, Map<?, ?>>> cursor = null;
            try (Connection connection = connectionProvider.get()) {
                final Table table = R.table(tableName);
                cursor = filter.orElse(t -> t).apply(table).changes().run(connection);

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

        final T oldValObj = oldVal != null ? MAPPER.convertValue(oldVal, clazz) : null;
        final T newValObj = newVal != null ? MAPPER.convertValue(newVal, clazz) : null;

        return new ChangeFeedElement<>(oldValObj, newValObj);
    }
}
