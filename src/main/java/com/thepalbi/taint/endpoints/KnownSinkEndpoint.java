package com.thepalbi.taint.endpoints;

import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

import java.util.Arrays;
import java.util.Set;

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
        if (knownSinkNames.contains(function.getFunctionData().getName())) {
            asList(arguments).stream()
                    .map(arg -> instrument.getTaint(arg))
                    .filter(b -> b.isTainted())
                    .forEach(offendingTaint -> instrument.registerOffense(offendingTaint));
        }
    }
}
