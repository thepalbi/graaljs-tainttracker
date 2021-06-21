package com.thepalbi.taint.propagators;

import com.thepalbi.taint.InputCapturerEventExecutionNode;
import com.thepalbi.taint.TaintTrackerInstrument;
import com.thepalbi.taint.model.TaintWithOrigin;

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
        TaintWithOrigin taint = instrument.getTaint(inputValues[0]);
        if (taint.isTainted()) {
            instrument.propagateTaint(result, taint);
        }
    }
}
