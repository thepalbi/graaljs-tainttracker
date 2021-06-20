package com.thepalbi.taint.endpoints;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

import static com.thepalbi.taint.TaintTrackerInstrument.trace;

public class TestTaintInjectorEndpoint extends FunctionCallEndpoint {

    private final TaintTrackerInstrument instrument;
    private final String callSourceCode;

    public TestTaintInjectorEndpoint(TaintTrackerInstrument instrument, EventContext ctx) {
        this.instrument = instrument;
        callSourceCode = ctx.getInstrumentedSourceSection().getCharacters().toString();
    }

    @Override
    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {
        if (callSourceCode.startsWith("createSensitive")) {
            // This function result produces taint
            trace("Tainting resulting object of type: %s", result.getClass());
            instrument.taint(result);
        }
    }
}
