package de.jakobjarosch.rethinkdb.orm.model;

public class IndexModel {

    private final boolean geo;
    private final String[] fields;

    public IndexModel(boolean geo, String[] fields) {
        this.geo = geo;
        this.fields = fields;
    }

    public boolean isGeo() {
        return geo;
    }

    public String[] getFields() {
        return fields;
    }
}
