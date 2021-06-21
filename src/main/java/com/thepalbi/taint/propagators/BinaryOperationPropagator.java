package com.thepalbi.taint.propagators;

import com.thepalbi.taint.InputCapturerEventExecutionNode;
import com.thepalbi.taint.TaintTrackerInstrument;
import com.thepalbi.taint.model.TaintWithOrigin;

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
        TaintWithOrigin taint1 = instrument.getTaint(inputValues[0]);
        TaintWithOrigin taint2 = instrument.getTaint(inputValues[1]);

        if (taint1.isTainted() || taint2.isTainted()) {
            // FIXME: Merge taint origins
            instrument.propagateTaint(result, taint1);
        }
    }
}
