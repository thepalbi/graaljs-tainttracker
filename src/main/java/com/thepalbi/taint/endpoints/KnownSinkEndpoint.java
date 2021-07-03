package com.thepalbi.taint.endpoints;

import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

import java.util.Set;

import static com.thepalbi.taint.TaintTrackerInstrument.trace;
import static java.util.Arrays.asList;

public class KnownSinkEndpoint extends FunctionCallEndpoint {
    private final Set<String> knownSinkNames;
    private final TaintTrackerInstrument instrument;

    public KnownSinkEndpoint(TaintTrackerInstrument instrument, Set<String> knownSinkNames) {
        this.instrument = instrument;
        this.knownSinkNames = knownSinkNames;
    }

    @Override
    protected void beforeCall(Object receiver, JSFunctionObject function, Object[] arguments) {
        // Maybe the function being called doesn't have a name argument. Use the function object itself to detect if it's a known sink, somehow!
        String functionName = function.getFunctionData().getName();
        if (knownSinkNames.contains(functionName)) {
            trace("Reached %s known sink. Evaluating if any argument is tainted!", functionName);
            asList(arguments).stream()
                    .map(arg -> instrument.getTaint(arg))
                    .filter(b -> b.isTainted())
                    .forEach(offendingTaint -> instrument.registerOffense(offendingTaint));
        }
    }
}
