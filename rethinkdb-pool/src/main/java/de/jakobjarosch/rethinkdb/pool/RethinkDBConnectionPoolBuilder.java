package de.jakobjarosch.rethinkdb.pool;


import com.rethinkdb.gen.exc.ReqlUserError;

public class RethinkDBConnectionPoolBuilder {

    private String hostname = "127.0.0.1";
    private int port = 28015;
    private String username = "admin";
    private String password = "";
    private String database = "test";

    private int maxConnections = 10;
    private int minFreeConnections = 1;
    private int maxFreeConnections = 5;

    public RethinkDBConnectionPoolBuilder hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public RethinkDBConnectionPoolBuilder port(int port) {
        if (port < 1 || port > 65535)
            throw new ReqlUserError("Constraint violated: 1 <= port <= 65535");
        this.port = port;
        return this;
    }


    public RethinkDBConnectionPoolBuilder username(String username) {
        this.username = username;
        return this;
    }


    public RethinkDBConnectionPoolBuilder password(String password) {
        this.password = password;
        return this;
    }


    public RethinkDBConnectionPoolBuilder database(String database) {
        this.database = database;
        return this;
    }

    public RethinkDBConnectionPoolBuilder maxConnections(int maxConnections) {
        checkConnectionConstraints(maxConnections, this.minFreeConnections, this.maxFreeConnections);
        this.maxConnections = maxConnections;
        return this;
    }

    public RethinkDBConnectionPoolBuilder minFreeConnections(int minFreeConnections) {
        checkConnectionConstraints(this.maxConnections, minFreeConnections, this.maxFreeConnections);
        this.minFreeConnections = minFreeConnections;
        return this;
    }

    public RethinkDBConnectionPoolBuilder maxFreeConnections(int maxFreeConnections) {
        checkConnectionConstraints(this.maxConnections, this.minFreeConnections, maxFreeConnections);
        this.maxFreeConnections = maxFreeConnections;
        return this;
    }

    public RethinkDBConnectionPool build() {
        return new RethinkDBConnectionPool(hostname, port, username, password, database, maxConnections, minFreeConnections, maxFreeConnections);
    }

    private void checkConnectionConstraints(int maxConnections, int minFreeConnections, int maxFreeConnections) {
        if (maxConnections < minFreeConnections)
            throw new ReqlUserError("Constraint violated: maxConnections >= minFreeConnections.");
        if (maxConnections < maxFreeConnections)
            throw new ReqlUserError("Constraint violated: maxConnections >= maxFreeConnections");
        if (maxFreeConnections < minFreeConnections)
            throw new ReqlUserError("Constraint violated: maxFreeConnections >= minFreeConnections");
    }
}
