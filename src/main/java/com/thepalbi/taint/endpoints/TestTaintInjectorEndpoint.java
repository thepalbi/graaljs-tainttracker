package com.thepalbi.taint.endpoints;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

import static com.thepalbi.taint.TaintTrackerInstrument.trace;

public class TestTaintInjectorEndpoint extends FunctionCallEndpoint {

    private static String TAINT_INJECTING_FUNCTION_PREFIX = "createSensitive";
    private final TaintTrackerInstrument instrument;
    @CompilerDirectives.CompilationFinal
    private final Boolean isCreateSensitive;

    public TestTaintInjectorEndpoint(TaintTrackerInstrument instrument, EventContext ctx) {
        String callSourceCode = ctx.getInstrumentedSourceSection().getCharacters().toString();
        this.instrument = instrument;
        isCreateSensitive = callSourceCode.startsWith(TAINT_INJECTING_FUNCTION_PREFIX);
    }

    @Override
    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {
        if (isCreateSensitive) {
            // This function result produces taint
            trace("Tainting resulting object of type: %s", result.getClass());
            instrument.taint(result, function.getFunctionData().getName());
        }
    }
}
