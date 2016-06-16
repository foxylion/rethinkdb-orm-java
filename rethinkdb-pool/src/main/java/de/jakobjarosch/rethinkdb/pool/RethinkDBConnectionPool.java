package de.jakobjarosch.rethinkdb.pool;


import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.gen.exc.ReqlError;
import com.rethinkdb.net.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class RethinkDBConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RethinkDBConnectionPool.class);

    class PoolMonitor extends Thread {

        PoolMonitor() {
            setName("RethinkDB-PoolMonitor");
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    updateConnectionPoolSize();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                LOGGER.info("PoolMonitor got interrupt, monitoring stopped.");
            }
        }
    }

    private Thread poolMonitor;

    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private final String database;

    private final int maxConnections;
    private final int minFreeConnections;
    private final int maxFreeConnections;

    private final Set<Connection> connections;
    private final BlockingQueue<Connection> freeConnections;

    RethinkDBConnectionPool(String hostname, int port, String username, String password, String database,
                            int maxConnections, int minFreeConnections, int maxFreeConnections) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.maxConnections = maxConnections;
        this.minFreeConnections = minFreeConnections;
        this.maxFreeConnections = maxFreeConnections;

        this.connections = new HashSet<>(maxConnections);
        this.freeConnections = new ArrayBlockingQueue<>(maxConnections);
    }

    public synchronized void start() {
        if (poolMonitor != null)
            throw new ReqlDriverError("Pool is already running.");

        LOGGER.debug("Starting pool... { maxConnections={}, minFreeConnections={}, maxFreeConnections={} }",
                maxConnections, minFreeConnections, maxFreeConnections);

        poolMonitor = new PoolMonitor();
        poolMonitor.start();
    }

    /**
     * @param timeout Timeout in seconds until the connections are force closed.
     */
    public synchronized void shutdown(int timeout) {
        if (poolMonitor == null)
            throw new ReqlDriverError("Pool is not running.");

        LOGGER.debug("Shutting down pool... { poolSize={}, freeConnections={} }", connections.size(), freeConnections.size());

        try {
            poolMonitor.interrupt();
            poolMonitor.join();
            poolMonitor = null;

            long timeoutStart = System.currentTimeMillis();

            while (!connections.isEmpty() && System.currentTimeMillis() - timeoutStart < timeout * 1000) {
                // There are open connections left and the we have still time to close some.

                while (!freeConnections.isEmpty()) {
                    closeConnection();
                }

                // Wait a moment and check again if there are some connections freed.
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted the shutdown process before timeout, force closing connections now.");
        }

        LOGGER.debug("Force closing remaining {} connections.", connections.size());

        connections.forEach(c -> c.close());
        connections.clear();
    }

    public boolean isRunning() {
        return poolMonitor != null;
    }

    public Provider<Connection> getProvider() {
        return () -> getConnection();
    }

    /**
     * @return Returns a free connection within 60 seconds.
     * @throws ReqlDriverError Throws error when no free connection is available within 60 seconds.
     */
    public Connection getConnection() {
        return getConnection(60);
    }

    /**
     * @param timeout Timeout in seconds.
     * @return Returns a free connection within the specified seconds.
     * @throws ReqlDriverError Throws error when no free connection is available within specified seconds.
     */
    public Connection getConnection(long timeout) {
        if (!isRunning())
            throw new ReqlDriverError("Connection pool is not running.");

        try {
            final Connection connection = freeConnections.poll(1, TimeUnit.SECONDS);
            return new PersistentConnection(connection, () -> {
                // Connection may already be removed from pool during shutdown
                if (connections.contains(connection))
                    freeConnections.offer(connection);
            });
        } catch (InterruptedException e) {
            throw new ReqlDriverError("Failed to get a free connection from pool. timeout=" + timeout);
        }
    }

    public synchronized void updateConnectionPoolSize() {
        try {
            LOGGER.trace("Checking for the need to adjust connection pool size... { poolSize={}, freeConnections={} }",
                    connections.size(), freeConnections.size());

            while (freeConnections.size() < minFreeConnections && connections.size() < maxConnections) {
                createConnection();
            }

            while (freeConnections.size() > maxFreeConnections) {
                closeConnection();
            }

            for (Connection connection : connections) {
                if (!connection.isOpen()) {
                    LOGGER.info("Found a closed connection, trying to re-establish connection...");
                    connection.reconnect();
                }
            }
        } catch (ReqlError e) {
            LOGGER.error("Failed to update connection pool, error while maintaining connections.", e);
        }
    }

    private void createConnection() {
        final Connection connection = RethinkDB.r.connection()
                .hostname(hostname)
                .port(port)
                .user(username, password)
                .db(database)
                .connect();

        connections.add(connection);
        freeConnections.offer(connection);

        LOGGER.debug("Created database connection. { poolSize={}, freeConnections={} }", connections.size(), freeConnections.size());
    }

    private void closeConnection() {
        final Connection connection = freeConnections.poll();
        // May return null when there is no more element in queue.
        if (connection != null) {
            connections.remove(connection);
            connection.close();
        }

        LOGGER.debug("Closed database connection. { poolSize={}, freeConnections={} }", connections.size(), freeConnections.size());
    }
}
