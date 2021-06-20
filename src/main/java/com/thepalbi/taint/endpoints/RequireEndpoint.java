package com.thepalbi.taint.endpoints;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
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
            if (arguments.length < 1) {
                // TODO: This should be a warning
                TaintTrackerInstrument.trace("require arguments are empty or not of type String. Arguments: %s",
                        arguments.length > 0 ? arguments[0] : "Empty");
                return;
            }
            // In here I could either cast, or take a toString(). Having seen some other types as first argument of a require,
            // like String and JSLazyString, opting for the former.
            String requireString = arguments[0].toString();
            // Just tainting requires whose Path start with lutRootDirectory
            if (requireString.startsWith(".")) {
                // Make full directory
                String absoluteRequirePath = Paths.get(sourcePath, requireString).toAbsolutePath().toString();
                System.out.printf("RequireEndpoint - Called require with absolute path: %s\n", absoluteRequirePath);
                if (absoluteRequirePath.startsWith(lutRootDirectory) && !absoluteRequirePath.contains("node_modules")) {
                    // TAINT
                    System.out.printf("RequireEndpoint - Injecting taint\n");
                    instrument.taint(result);
                }
            }
        }
    }

}
