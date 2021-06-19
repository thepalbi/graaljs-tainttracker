package com.oracle.truffle.st.propagators;

import com.oracle.truffle.st.InputCapturerEventExecutionNode;
import com.oracle.truffle.st.TaintTrackerInstrument;

public class UnaryOperationPropagator extends InputCapturerEventExecutionNode {

    private final TaintTrackerInstrument instrument;

    public UnaryOperationPropagator(TaintTrackerInstrument instrument) {
        this.instrument = instrument;
    }

    @Override
    protected void beforeEvaluation(Object[] inputValues) {

    }

    @Override
    protected void afterEvaluation(Object[] inputValues, Object result) {
        assert inputValues.length == 1;
        if (instrument.isTainted(inputValues[0])) {
            instrument.taint(result);
        }
    }
}
