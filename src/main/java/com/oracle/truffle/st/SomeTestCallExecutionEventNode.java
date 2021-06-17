package com.oracle.truffle.st;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;

import java.io.File;
import java.nio.file.Paths;

public class SomeTestCallExecutionEventNode extends ExecutionEventNode {

    @CompilerDirectives.CompilationFinal
    private boolean isRequireCall;
    private final String path;
    private final String libraryRootDir;
    private final SimpleCoverageInstrument instrument;


    public SomeTestCallExecutionEventNode(boolean isRequireCall, String path, String libraryRootDir, SimpleCoverageInstrument instrument) {
        this.isRequireCall = isRequireCall;
        this.path = path;
        this.libraryRootDir = libraryRootDir;
        this.instrument = instrument;
    }


    @Override
    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
        if (isRequireCall) {
            saveInputValue(frame, inputIndex, inputValue);
        }
    }

    @Override
    protected void onReturnValue(VirtualFrame frame, Object result) {
        if (isRequireCall) {
            Object[] inputValues = getSavedInputValues(frame);
            if (inputValues.length == 3 && inputValues[2] instanceof String) {
                String requireString = (String) inputValues[2];
                if (requireString.startsWith(".")) {
                    // Must be a relative require
                    File pathFile = new File(path);
                    String absoluteRequirePath = Paths.get(pathFile.getParent(), requireString).toString();
                    boolean isEntryPoint = absoluteRequirePath.startsWith(libraryRootDir) && !absoluteRequirePath.contains("node_modules");
                    instrument.addRequire("", absoluteRequirePath, isEntryPoint);
                    // Add result object as seen
                    instrument.addAsSeen(result,
                            String.format("result of require(\"%s\")", absoluteRequirePath), true);
                } else {
                    instrument.addRequire(path, requireString, false);
                }
            }
            // Since it's a require("") call
//                                    if (inputValues.length != 3) {
//                                        System.out.printf("Found require inputValues with: %d lastElement[%s]\n", inputValues.length,
//                                                inputValues.length > 0 ? inputValues[inputValues.length - 1].toString() : "EMPTY");
//
//                                        return;
//                                    }
        }
    }
}
