package com.thepalbi.taint.endpoints;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.thepalbi.taint.TaintTrackerInstrument;

import java.nio.file.Paths;


public class RequireEndpoint extends FunctionCallEndpoint {

    private final TaintTrackerInstrument instrument;
    private final String sourcePath;
    private final String lutRootDirectory;
    @CompilerDirectives.CompilationFinal
    private boolean isRequire;

    public RequireEndpoint(TaintTrackerInstrument instrument, EventContext ctx, String lutRootDirectory) {
        this.instrument = instrument;
        sourcePath = ctx.getInstrumentedSourceSection().getSource().getPath();
        isRequire = ctx.getInstrumentedSourceSection().getCharacters().toString().startsWith("require");
        this.lutRootDirectory = lutRootDirectory;
    }

    @Override
    protected void afterCall(Object receiver, JSFunctionObject function, Object[] arguments, Object result) {
        if (isRequire) {
            assert arguments[0] instanceof String;
            String requireString = (String) arguments[0];
            System.out.printf("%s, %s, %s\n",
                    sourcePath,
                    arguments[0],
                    result.getClass().getName());
            // Just tainting requires whose Path start with lutRootDirectory
            if (requireString.startsWith(".")) {
                // Make full directory
                String absoluteRequirePath = Paths.get(sourcePath, requireString).toAbsolutePath().toString();
                TaintTrackerInstrument.trace("RequireEndpoint - Called require with abolsute path: %s", absoluteRequirePath);
                if (absoluteRequirePath.startsWith(lutRootDirectory)) {
                    // TAINT
                    instrument.taint(result);
                }
            }
        }
    }

}
