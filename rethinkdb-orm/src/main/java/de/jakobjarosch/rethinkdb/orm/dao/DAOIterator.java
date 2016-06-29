package de.jakobjarosch.rethinkdb.orm.dao;


import com.rethinkdb.net.Cursor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;


public class DAOIterator<T> implements Iterator<T>, Closeable {

    private final Iterator<?> iterator;
    private final Optional<Closeable> closable;
    private final Class<T> clazz;
    private final ModelMapper mapper;

    public DAOIterator(Iterator<?> iterator, Class<T> clazz, ModelMapper mapper) {
        this.iterator = iterator;
        this.closable = Optional.empty();
        this.clazz = clazz;
        this.mapper = mapper;
    }

    public DAOIterator(Cursor<?> cursor, Class<T> clazz, ModelMapper mapper) {
        this.iterator = cursor;
        this.closable = Optional.of(cursor::close);
        this.clazz = clazz;
        this.mapper = mapper;
    }

    @Override
    public void close() throws IOException {
        if (closable.isPresent()) {
            closable.get().close();
        }
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return mapper.map((Map) iterator.next(), clazz);
    }
}
