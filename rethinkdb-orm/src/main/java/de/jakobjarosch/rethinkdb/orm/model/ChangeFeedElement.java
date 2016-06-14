package de.jakobjarosch.rethinkdb.orm.model;

import java.util.Optional;

public class ChangeFeedElement<T> {

    private final Optional<T> oldValue;
    private final Optional<T> newValue;

    public ChangeFeedElement(T oldValue, T newValue) {
        this.oldValue = Optional.ofNullable(oldValue);
        this.newValue = Optional.ofNullable(newValue);
    }

    public Optional<T> getOldValue() {
        return oldValue;
    }

    public Optional<T> getNewValue() {
        return newValue;
    }
}
