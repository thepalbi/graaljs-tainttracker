package com.thepalbi.taint.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;

public class TaintWithOrigin {
    private final List<String> origins;

    public TaintWithOrigin(String origin) {
        this.origins = singletonList(origin);
    }

    TaintWithOrigin(List<String> origins) {
        this.origins = origins;
    }

    public boolean isTainted() {
        return true;
    }

    public List<String> getOrigins() {
        return origins;
    }

    public static TaintWithOrigin merge(TaintWithOrigin taint1, TaintWithOrigin taint2) {
        List<String> mergedOrigins = new LinkedList<>();
        mergedOrigins.addAll(taint1.origins);
        mergedOrigins.addAll(taint2.origins);
        return new TaintWithOrigin(mergedOrigins);
    }

    public static class NoTaint extends TaintWithOrigin {

        private static NoTaint instance;

        static {
            instance = new NoTaint();
        }

        public static TaintWithOrigin getInstance() {
            return instance;
        }

        NoTaint() {
            super(Collections.emptyList());
        }

        @Override
        public boolean isTainted() {
            return false;
        }
    }
}
