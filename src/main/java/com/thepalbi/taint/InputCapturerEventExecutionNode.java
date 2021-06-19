package com.thepalbi.taint;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;

public abstract class InputCapturerEventExecutionNode extends ExecutionEventNode {

    protected abstract void beforeEvaluation(Object[] inputValues);

    protected abstract void afterEvaluation(Object[] inputValues, Object result);

    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
        saveInputValue(frame, inputIndex, inputValue);
        if (inputIndex == getInputCount() - 1) {
            try {
                beforeEvaluation(getSavedInputValues(frame));
            } catch (Exception e) {
                System.err.printf("onInputValue - Catch JSException in class %s: %s\n", getClass().getName(), e.getMessage());
                throw e;
            }
        }
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        try {
            afterEvaluation(getSavedInputValues(frame), result);
        } catch (Exception e) {
            System.err.printf("onReturnValue - Catch JSException in class %s: %s\n", getClass().getName(), e.getMessage());
            throw e;
        }
    }
}
