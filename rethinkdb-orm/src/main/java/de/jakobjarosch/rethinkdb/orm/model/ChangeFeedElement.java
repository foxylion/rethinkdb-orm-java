package de.jakobjarosch.rethinkdb.orm.model;

import com.google.common.base.MoreObjects;

import java.util.Objects;
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

    @Override
    public int hashCode() {
        return Objects.hash(oldValue, newValue);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ChangeFeedElement))
            return false;
        ChangeFeedElement cfe = (ChangeFeedElement) o;
        return Objects.equals(this.getOldValue(), cfe.getOldValue()) &&
                Objects.equals(this.getNewValue(), cfe.getNewValue());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("oldValue", getOldValue())
                .add("newValue", getNewValue())
                .toString();
    }
}
