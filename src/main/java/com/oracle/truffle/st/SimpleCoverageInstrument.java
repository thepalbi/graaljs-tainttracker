/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.st;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Example for simple version of an expression coverage instrument.
 * <p>
 * The instrument {@link #coverageMap keeps track} of all loaded {@link SourceSection}s and all
 * coverd (i.e. executed) {@link SourceSection}s for each {@link Source}. At the end of the
 * execution this information can be used to calculate coverage.
 * <p>
 * The instrument is registered with the Truffle framework using the {@link Registration}
 * annotation. The annotation specifies a unique {@link Registration#id}, a human readable
 * {@link Registration#name} and {@link Registration#version} for the instrument. It also specifies
 * all service classes that the instrument exports to other instruments and, exceptionally, tests.
 * In this case the instrument itself is exported as a service and used in the
 * SimpleCoverageInstrumentTest.
 * <p>
 * NOTE: Fot the registration annotation to work the truffle dsl processor must be used (i.e. Must
 * be a dependency. This is so in this maven project, as can be seen in the pom file.
 */
@Registration(id = SimpleCoverageInstrument.ID, name = "Simple Code Coverage", version = "0.1", services = SimpleCoverageInstrument.class)
public final class SimpleCoverageInstrument extends TruffleInstrument {

    // @formatter:off
    /**
     * Look at {@link #onCreate(Env)} and {@link #getOptionDescriptors()} for more info.
     */
    @Option(name = "", help = "Enable Simple Coverage (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

    /**
     * Look at {@link #onCreate(Env)} and {@link #getOptionDescriptors()} for more info.
     */
    @Option(name = "PrintCoverage", help = "Print coverage to stdout on process exit (default: true).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> PRINT_COVERAGE = new OptionKey<>(true);

    // @formatter:off
    /**
     * Look at {@link #onCreate(Env)} and {@link #getOptionDescriptors()} for more info.
     */
    @Option(name = "LibraryRootDir", help = "Library root dir to consider requires as entry points (defaults to spawn).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> LIBRARY_ROOT_DIR = new OptionKey<>("/Users/pabbalbi/tesis/slim-taser/test_libs/spawn");
    // @formatter:on

    public static final String ID = "simple-code-coverage";

    /**
     * The instrument keeps a mapping between a {@link Source} and {@link Coverage coverage} data
     * for that source. Coverage tracks loaded and covered {@link SourceSection} during execution.
     */
    final Map<Source, Coverage> coverageMap = new HashMap<>();

    final Set<RequireQuery> collectedRequires = new HashSet<>();

    final Map<Object, CounterWithNote> seenTimes = new HashMap<>();

    class CounterWithNote {
        String note;
        Integer count;

        public CounterWithNote(String note) {
            this.note = note;
            count = 0;
        }
    }

    public synchronized Map<Source, Coverage> getCoverageMap() {
        return Collections.unmodifiableMap(coverageMap);
    }

    public synchronized Set<RequireQuery> getCollectedRequires() {
        return Collections.unmodifiableSet(collectedRequires);
    }

    /**
     * Each instrument must override the
     * {@link TruffleInstrument#onCreate(com.oracle.truffle.api.instrumentation.TruffleInstrument.Env)}
     * method.
     * <p>
     * This method is used to properly initialize the instrument. A common practice is to use the
     * {@link Option} system to enable and configure the instrument, as is done in this method.
     * Defining {@link Option}s as is shown in {@link #ENABLED} and {@link #PRINT_COVERAGE}, and
     * their usage can be seen in the SimpleCoverageInstrumentTest when the context is being
     * created. Using them from the command line is shown in the simpletool.sh script.
     *
     * @param env the environment for the instrument. Allows us to read the {@link Option}s, input
     *            and output streams to be used for reading and writing, as well as
     *            {@link Env#registerService(java.lang.Object) registering} and
     *            {@link Env#lookup(com.oracle.truffle.api.InstrumentInfo, java.lang.Class) looking
     *            up} services.
     */
    @Override
    protected void onCreate(final Env env) {
        final OptionValues options = env.getOptions();
        if (ENABLED.getValue(options)) {
            enable(env);
            env.registerService(this);
        }
    }

    /**
     * Enable the instrument.
     * <p>
     * In this method we enable and configure the instrument. We do this by first creating a
     * {@link SourceSectionFilter} instance in order to specify exactly which parts of the source
     * code we are interested in. In this particular case, we are interested in expressions. Since
     * Truffle Instruments are language agnostic, they rely on language implementers to tag AST
     * nodes with adequate tags. This, we tell our {@link SourceSectionFilter.Builder} that we are
     * care about AST nodes {@link SourceSectionFilter.Builder#tagIs(java.lang.Class...) tagged}
     * with {@link ExpressionTag}. We also tell it we don't care about AST nodes
     * {@link SourceSectionFilter.Builder#includeInternal(boolean) internal} to languages.
     * <p>
     * After than, we use the {@link Env enviroment} to obtain the {@link Instrumenter}, which
     * allows us to specify in which way we wish to instrument the AST.
     * <p>
     * Firstly, we
     * {@link Instrumenter#attachLoadSourceListener(com.oracle.truffle.api.instrumentation.SourceFilter, com.oracle.truffle.api.instrumentation.LoadSourceListener, boolean)
     * attach attach} our own {@link GatherSourceSectionsListener listener} to loading source
     * section events. Each the a {@link SourceSection} is loaded, our listener is notified, so our
     * instrument is always aware of all loaded code. Note that we have specified the filter defined
     * earlier as a constraint, so we are not notified if internal code is loaded.
     * <p>
     * Secondly, we
     * {@link Instrumenter#attachExecutionEventFactory(com.oracle.truffle.api.instrumentation.SourceSectionFilter, com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory)
     * attach} our {@link CoverageEventFactory node factory} using the same filter. This factory
     * produces {@link Node Truffle Nodes} that will be inserted into the AST at positions specified
     * by the filter. Each of the inserted nodes will, once executed, remove the corresponding
     * source section from the {@link #coverageMap set of unexecuted source sections}.
     *
     * @param env The environment, used to get the {@link Instrumenter}
     */
    private void enable(final Env env) {
        SourceSectionFilter properyReadFilter = SourceSectionFilter.newBuilder().tagIs(JSTags.ReadPropertyTag.class).build();
        SourceSectionFilter callsFilter = SourceSectionFilter.newBuilder().tagIs(JSTags.FunctionCallTag.class).build();
        SourceSectionFilter inputFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.ExpressionTag.class, JSTags.InputNodeTag.class).build();
        Instrumenter instrumenter = env.getInstrumenter();
        instrumenter.attachExecutionEventFactory(
                properyReadFilter,
                inputFilter,
                ctx -> new PropertyReadNode(this, ctx));
        instrumenter.attachExecutionEventFactory(
                callsFilter,
                inputFilter,
                new ExecutionEventNodeFactory() {
                    @Override
                    public ExecutionEventNode create(EventContext context) {
                        boolean doIsRequireCall = context.getInstrumentedSourceSection().getCharacters().toString().startsWith("require(");
                        String pathToSource = context.getInstrumentedSourceSection().getSource().getPath();
                        return new ExecutionEventNode() {
                            @CompilerDirectives.CompilationFinal
                            private boolean isRequireCall = doIsRequireCall;
                            private final String path = pathToSource;

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
                                            boolean isEntryPoint = absoluteRequirePath.startsWith(LIBRARY_ROOT_DIR.getValue(env.getOptions())) && !absoluteRequirePath.contains("node_modules");
                                            SimpleCoverageInstrument.this.addRequire("", absoluteRequirePath, isEntryPoint);
                                            // Add result object as seen
                                            SimpleCoverageInstrument.this.addAsSeen(result,
                                                    String.format("result of require(\"%s\")", absoluteRequirePath), true);
                                        } else {
                                            SimpleCoverageInstrument.this.addRequire(path, requireString, false);
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
                        };
                    }
                });
    }

    /**
     * Ensures that the coverage info gathered by the instrument is printed at the end of execution.
     *
     * @param env
     */
    @Override
    protected void onDispose(Env env) {
        if (PRINT_COVERAGE.getValue(env.getOptions())) {
            printResults(env);
        }
    }

    /**
     * Print the coverage results for each source.
     * <p>
     * The printing is one the the output stream specified by the {@link Env
     * enviroment}.
     *
     * @param env
     */
    private synchronized void printResults(final Env env) {
        final PrintStream printStream = new PrintStream(env.out());
        printStream.println("==");
        printStream.println("Listing all collected requires:");
        for (RequireQuery collectedRequire : collectedRequires) {
            printStream.printf("isEntryPoints: %s - called on %s, require(\"%s\")\n",
                    collectedRequire.isEntryPoint(),
                    collectedRequire.getPath(),
                    collectedRequire.getQuery());
        }
        printStream.println("==");
        printStream.println("Seen objects:");
        for (Object key : seenTimes.keySet()) {
            CounterWithNote seenThing = seenTimes.get(key);
            printStream.printf("%s seen %d times\n", seenThing.note, seenThing.count);
        }
    }

    private void printResult(PrintStream printStream, Source source) {
        String path = source.getPath();
        int lineCount = source.getLineCount();
        Set<Integer> nonCoveredLineNumbers = nonCoveredLineNumbers(source);
        Set<Integer> loadedLineNumbers = coverageMap.get(source).loadedLineNumbers();
        double coveredPercentage = 100 * ((double) loadedLineNumbers.size() - nonCoveredLineNumbers.size()) / lineCount;
        printStream.println("==");
        printStream.println("Coverage of " + path + " is " + String.format("%.2f%%", coveredPercentage));
        for (int i = 1; i <= source.getLineCount(); i++) {
            char covered = getCoverageCharacter(nonCoveredLineNumbers, loadedLineNumbers, i);
            printStream.println(String.format("%s %s", covered, source.getCharacters(i)));
        }
    }

    private static char getCoverageCharacter(Set<Integer> nonCoveredLineNumbers, Set<Integer> loadedLineNumbers, int i) {
        if (loadedLineNumbers.contains(i)) {
            return nonCoveredLineNumbers.contains(i) ? '-' : '+';
        } else {
            return ' ';
        }
    }

    /**
     * @param source
     * @return A sorted list of line numbers for not-yet-covered lines of source code in the given
     * {@link Source}
     */
    public synchronized Set<Integer> nonCoveredLineNumbers(final Source source) {
        return coverageMap.get(source).nonCoveredLineNumbers();
    }

    /**
     * Which {@link OptionDescriptors} are used for this instrument.
     * <p>
     * If the {@link TruffleInstrument} uses {@link Option}s, it is nesesery to specify which
     * {@link Option}s. The {@link OptionDescriptors} is automatically generated from this class due
     * to the {@link Option} annotation. In our case, this is the
     * {@code SimpleCodeCoverageInstrumentOptionDescriptors} class.
     *
     * @return The class generated by the {@link Option.Group} annotation
     */
    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new SimpleCoverageInstrumentOptionDescriptors();
    }

    /**
     * Called when a new {@link SourceSection} is loaded. We can update our {@link #coverageMap}.
     *
     * @param sourceSection the newly loaded {@link SourceSection}
     */
    synchronized void addLoaded(SourceSection sourceSection) {
        final Coverage coverage = coverageMap.computeIfAbsent(sourceSection.getSource(), new Function<Source, Coverage>() {
            @Override
            public Coverage apply(Source s) {
                return new Coverage();
            }
        });
        coverage.addLoaded(sourceSection);
    }

    /**
     * Called after a {@link SourceSection} is executed, and thus covered. We can update our
     * {@link #coverageMap}.
     *
     * @param sourceSection the executed {@link SourceSection}
     */
    synchronized void addCovered(SourceSection sourceSection) {
        final Coverage coverage = coverageMap.get(sourceSection.getSource());
        coverage.addCovered(sourceSection);
    }

    synchronized void addRequire(String path, String query, boolean isEntryPoint) {
        collectedRequires.add(new RequireQuery(path, query, isEntryPoint));
    }

    synchronized void addAsSeen(Object obj, String note, boolean createOk) {
        CounterWithNote notedCounter = seenTimes.get(obj);
        if (notedCounter == null && !createOk) {
            return;
        }
        if (notedCounter == null) notedCounter = new CounterWithNote(note);

        notedCounter.count++;
        seenTimes.put(obj, notedCounter);
    }
}
