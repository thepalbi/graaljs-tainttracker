package com.thepalbi.taint.endpoints;

import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

public class EntryPointTaintInjectorEndpoint extends FunctionCallEndpoint {
    private final TaintTrackerInstrument instrument;

    public EntryPointTaintInjectorEndpoint(TaintTrackerInstrument instrument) {
        this.instrument = instrument;
    }

    @Override
    protected void beforeCall(Object receiver, JSFunctionObject function, Object[] arguments) {
        String entryPointName = instrument.getEntryPointNameOrNull(function);
        if (entryPointName != null) {
            // Taint all arguments entering the module under test
            for (Object arg : arguments) {
                instrument.taint(arg, entryPointName);
            }
            System.out.printf("Tainted %d objects, entering through %s\n", arguments.length, entryPointName);
        }
    }
}
