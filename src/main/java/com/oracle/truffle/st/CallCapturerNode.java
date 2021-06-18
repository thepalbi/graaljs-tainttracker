package com.oracle.truffle.st;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.source.SourceSection;

public class CallCapturerNode extends ExecutionEventNode {

    private final String callSourceCode;
    private final String prefix;
    private final TaintTrackerInstrument instrument;

    CallCapturerNode(TaintTrackerInstrument instrument, SourceSection instrumentedSourceSection, String prefix) {
        this.instrument = instrument;
        this.callSourceCode = instrumentedSourceSection.getCharacters().toString();
        this.prefix = prefix;
    }

    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
        saveInputValue(frame, inputIndex, inputValue);
        System.out.printf("%s - %s > onInputValue %d of %d - %s\n", prefix, callSourceCode, inputIndex + 1, getInputCount(), inputValue.getClass().getName());
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        System.out.printf("%s - %s > onEnter\n", prefix, callSourceCode);
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        System.out.printf("%s - %s > onReturnValue savedArgCount %d\n", prefix, callSourceCode, getSavedInputValues(frame).length);
    }
}
