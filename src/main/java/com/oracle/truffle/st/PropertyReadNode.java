package com.oracle.truffle.st;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

import static java.lang.String.format;

public class PropertyReadNode extends ExecutionEventNode {
    private final SimpleCoverageInstrument instrument;
    private final Object field;

    public PropertyReadNode(SimpleCoverageInstrument instrument, EventContext ctx) {
        this.instrument = instrument;
        field = getField(ctx);
    }

    private Object getField(EventContext ctx) {
        try {
            return InteropLibrary.getFactory().getUncached().readMember((InstrumentableNode) ctx.getInstrumentedNode(), "key");
        } catch (Exception e) {
            System.out.printf("Failed to get key field from instrumented node!\n");
        }
        return null;
    }

    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
        saveInputValue(frame, inputIndex, inputValue);
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        Object baseObject = getSavedInputValues(frame)[0];
        instrument.addAsSeen(baseObject, "base of prop read", false);
        instrument.addAsSeen(result, format("result of %s[%s]", baseObject.toString(), field != null ? field.toString() : "null"), false);
    }
}
