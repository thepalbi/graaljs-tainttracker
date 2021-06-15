package com.oracle.truffle.st;

import java.util.Objects;

public class RequireQuery {
    private final String path;
    private final String query;

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

    public RequireQuery(String path, String query) {
        this.path = path;
        this.query = query;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }


}
