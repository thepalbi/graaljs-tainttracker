package com.thepalbi.taint.meta;

import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

import java.util.HashMap;
import java.util.Map;

public class SimpleMetaStore<T> {
    private Map<Object, T> nativeTypeStore = new HashMap<>();
    private final T defaultValue;
    private Integer taintedCount = 0;

    public SimpleMetaStore(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    // Using same meta storage key as TASER: https://github.com/cs-au-dk/taser/blob/master/src/MetaHelper.ts#L29
    private final String jsMetaStoreKey = "mapping23$^42";

    public void store(Object container, T value) {
        if (container instanceof JSDynamicObject) {
            JSDynamicObject jContainer = (JSDynamicObject) container;
            jContainer.defineOwnProperty(jsMetaStoreKey, PropertyDescriptor.createData(value), true);
        } else {
            nativeTypeStore.put(container, value);
        }
        taintedCount++;
    }

    public T retrieve(Object container) {
        if (container instanceof JSDynamicObject) {
            JSDynamicObject jContainer = (JSDynamicObject) container;
            PropertyDescriptor metaPropertyDesc = jContainer.getOwnProperty(jsMetaStoreKey);
            if (metaPropertyDesc == null) {
                return defaultValue;
            }
            return (T) metaPropertyDesc.getValue();
        } else {
            return nativeTypeStore.getOrDefault(container, defaultValue);
        }
    }

    public Integer getTaintedCount() {
        return taintedCount;
    }
}
