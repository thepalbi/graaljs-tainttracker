package com.oracle.truffle.st;

import java.util.Objects;

public class RequireQuery {
    private final String path;
    private final String query;
    private final boolean isEntryPoint;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequireQuery that = (RequireQuery) o;
        return Objects.equals(path, that.path) && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, query);
    }

    public RequireQuery(String path, String query, boolean isEntryPoint) {
        this.path = path;
        this.query = query;
        this.isEntryPoint = isEntryPoint;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }


    public boolean isEntryPoint() {
        return isEntryPoint;
    }
}
