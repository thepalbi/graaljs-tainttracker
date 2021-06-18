package com.oracle.truffle.st.propagators;

import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.st.InputCapturerEventExecutionNode;
import com.oracle.truffle.st.TaintTrackerInstrument;

public class PropReadPropagator extends InputCapturerEventExecutionNode {
    private final TaintTrackerInstrument instrument;

    public PropReadPropagator(TaintTrackerInstrument instrument) {
        this.instrument = instrument;
    }


    @Override
    protected void beforeEvaluation(Object[] inputValues) {

    }

    @Override
    protected void afterEvaluation(Object[] inputValues, Object result) {
        // TODO: Discover how to find property name being read
        // inputValues[0] is the base of the prop read AST node
        assert inputValues.length == 1;

        Object base = inputValues[0];

        // Avoiding global.sth prop reads
        if (!(base instanceof JSGlobalObject) && instrument.isTainted(base)) {
            instrument.taint(result);
        }
    }
}
