package com.oracle.truffle.st;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.source.SourceSection;

public class CallCapturerNode extends ExecutionEventNode {

    private final String callSourceCode;
    private final SimpleCoverageInstrument instrument;

    CallCapturerNode(SimpleCoverageInstrument instrument, SourceSection instrumentedSourceSection) {
        this.instrument = instrument;
        this.callSourceCode = instrumentedSourceSection.getCharacters().toString();
    }

    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
        saveInputValue(frame, inputIndex, inputValue);
        System.out.printf("%s > onInputValue %d\n", callSourceCode, inputIndex);
    }

    @Override
    protected void onEnter(VirtualFrame frame) {
        System.out.printf("%s > onEnter\n", callSourceCode);
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        System.out.printf("%s > onReturnValue savedArgCount %d\n", callSourceCode, getSavedInputValues(frame).length);
    }
}
