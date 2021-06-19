package com.oracle.truffle.st.endpoints;

import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.st.InputCapturerEventExecutionNode;

public abstract class FunctionCallEndpoint extends InputCapturerEventExecutionNode {

    private static final int RECEIVER_INPUT_INDEX = 0;
    private static final int FUNCTION_INPUT_INDEX = 1;
    private Object receiver;
    private JSFunctionObject function;
    private Object[] arguments;

    protected void beforeCall(Object receiver, JSFunctionObject function, Object[] arguments) {

    }

    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {

    }

    private Object[] takeLastN(Object[] vals, int n) {
        assert vals.length >= n;
        Object[] res = new Object[n];
        for (int i = 0; i < n; i++) {
            res[i] = vals[vals.length - n + i];
        }
        return res;
    }

    @Override
    protected void beforeEvaluation(Object[] inputValues) {
        receiver = inputValues[RECEIVER_INPUT_INDEX];
        function = (JSFunctionObject) inputValues[FUNCTION_INPUT_INDEX];
        arguments = takeLastN(inputValues, inputValues.length - 2);
        beforeCall(receiver, function, arguments);
    }

    @Override
    protected void afterEvaluation(Object[] inputValues, Object result) {
        afterCall(receiver, function, arguments, result);
    }
}
