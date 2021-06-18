package com.oracle.truffle.st.propagators;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;


public class RequirePropagator extends FunctionCallPropagator {

    private final String sourcePath;
    @CompilerDirectives.CompilationFinal
    private boolean isRequire;

    public RequirePropagator(EventContext ctx) {
        sourcePath = ctx.getInstrumentedSourceSection().getSource().getPath();
        isRequire = ctx.getInstrumentedSourceSection().getCharacters().toString().startsWith("require");
    }

    @Override
    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {
        if (isRequire) {
            assert arguments[0] instanceof String;
            System.out.printf("%s, %s, %s\n",
                    sourcePath,
                    arguments[0],
                    result.getClass().getName());
        }
    }

}
