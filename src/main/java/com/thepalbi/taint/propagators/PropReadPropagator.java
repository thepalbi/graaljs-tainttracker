package com.thepalbi.taint.propagators;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.thepalbi.taint.InputCapturerEventExecutionNode;
import com.thepalbi.taint.TaintTrackerInstrument;

import static com.thepalbi.taint.TaintTrackerInstrument.trace;


public class PropReadPropagator extends InputCapturerEventExecutionNode {
    public static final String PROPERTY_READ_KEY = "key";
    private final TaintTrackerInstrument instrument;
    private final Object propertyKey;

    public PropReadPropagator(TaintTrackerInstrument instrument, EventContext ctx) {
        this.instrument = instrument;

        // Try to read key like NodeProf
        // https://github.com/Haiyang-Sun/nodeprof.js/blob/c513652ba0845667badf109278ca60e17bd3f3ac/src/ch.usi.inf.nodeprof/src/ch/usi/inf/nodeprof/handlers/BaseEventHandlerNode.java#L141
        InstrumentableNode node = (InstrumentableNode) ctx.getInstrumentedNode();
        propertyKey = readMemberOrNull(node.getNodeObject(), PROPERTY_READ_KEY);
        if (propertyKey != null) {
            trace("PropReadPropagator#constructor - Property being read: %s", propertyKey);
        } else {
            trace("PropReadPropagator#constructor - Failed to read property key");
        }
    }

    private Object readMemberOrNull(Object base, String member) {
        try {
            return InteropLibrary.getFactory().getUncached().readMember(base, member);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void beforeEvaluation(Object[] inputValues) {

    }

    @Override
    protected void afterEvaluation(Object[] inputValues, Object result) {
        // TODO: Discover how to find property name being read
        // inputValues[0] is the base of the prop read AST node
        assert inputValues.length == 1;

        Object base = inputValues[0];

        // Avoiding global.sth prop reads
        if (!(base instanceof JSGlobalObject) && instrument.isTainted(base)) {
            instrument.taint(result);
        }
    }
}
