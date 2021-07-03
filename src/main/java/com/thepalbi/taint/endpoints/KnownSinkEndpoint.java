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
    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {
        // Evaluation taint violations in afterCall, since the argument may be resolved in the following order:
        // For sth like console.log(obj.f)
        // 1. invocation console.log - beforeCall
        // 2. propRead obj.f - beforeEvaluation
        // 3. propRead obj.f - afterEvaluation
        // 4. invocation console.log - afterCall

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
