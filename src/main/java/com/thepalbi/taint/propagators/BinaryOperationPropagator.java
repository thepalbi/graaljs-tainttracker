package com.thepalbi.taint.propagators;

import com.thepalbi.taint.InputCapturerEventExecutionNode;
import com.thepalbi.taint.TaintTrackerInstrument;

public class BinaryOperationPropagator extends InputCapturerEventExecutionNode {
    private final TaintTrackerInstrument instrument;

    public BinaryOperationPropagator(TaintTrackerInstrument instrument) {
        this.instrument = instrument;
    }

    @Override
    protected void beforeEvaluation(Object[] inputValues) {
    }

    @Override
    protected void afterEvaluation(Object[] inputValues, Object result) {
        assert inputValues.length == 2;

        // retrieve taint from both input values
        Boolean taint1 = instrument.isTainted(inputValues[0]);
        Boolean taint2 = instrument.isTainted(inputValues[1]);

        if (taint1 || taint2) {
            instrument.taint(result);
        }
    }
}
