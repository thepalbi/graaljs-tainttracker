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
package com.thepalbi.taint;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Nullish;
import com.thepalbi.taint.endpoints.EntryPointTaintInjectorEndpoint;
import com.thepalbi.taint.endpoints.KnownSinkEndpoint;
import com.thepalbi.taint.endpoints.RequireEndpoint;
import com.thepalbi.taint.endpoints.TestTaintInjectorEndpoint;
import com.thepalbi.taint.meta.SimpleMetaStore;
import com.thepalbi.taint.model.TaintWithOrigin;
import com.thepalbi.taint.propagators.BinaryOperationPropagator;
import com.thepalbi.taint.propagators.PropReadPropagator;
import com.thepalbi.taint.propagators.UnaryOperationPropagator;
import org.graalvm.options.*;

import java.io.PrintStream;
import java.util.*;

/**
 * Example for simple version of an expression coverage instrument.
 * <p>
 * The instrument of all loaded {@link SourceSection}s and all coverd (i.e. executed) {@link SourceSection}s for each
 * {@link Source}. At the end of the execution this information can be used to calculate coverage.
 * <p>
 * The instrument is registered with the Truffle framework using the {@link Registration} annotation. The annotation
 * specifies a unique {@link Registration#id}, a human readable {@link Registration#name} and {@link
 * Registration#version} for the instrument. It also specifies all service classes that the instrument exports to other
 * instruments and, exceptionally, tests. In this case the instrument itself is exported as a service and used in the
 * SimpleCoverageInstrumentTest.
 * <p>
 * NOTE: Fot the registration annotation to work the truffle dsl processor must be used (i.e. Must be a dependency. This
 * is so in this maven project, as can be seen in the pom file.
 */
@Registration(id = TaintTrackerInstrument.ID, name = "Simple Taint Tracker", version = "0.1", services = TaintTrackerInstrument.class)
public final class TaintTrackerInstrument extends TruffleInstrument {

    private List<TaintWithOrigin> offendingTaints = new LinkedList<>();
    private SimpleMetaStore<TaintWithOrigin> metaStore = new SimpleMetaStore(TaintWithOrigin.NoTaint.getInstance());
    private Map<JSFunctionObject, String> entryPoints = new HashMap<>();

    // @formatter:off
    /**
     * Look at {@link #onCreate(Env)} and {@link #getOptionDescriptors()} for more info.
     */
    @Option(name = "", help = "Enable Simple Coverage (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

    // @formatter:off
    /**
     * Look at {@link #onCreate(Env)} and {@link #getOptionDescriptors()} for more info.
     */
    @Option(name = "LibraryRootDir", help = "Library root dir to consider requires as entry points (defaults to spawn).", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> LIBRARY_ROOT_DIR = new OptionKey<>("/Users/pabbalbi/tesis/slim-taser/test_libs/spawn");

    @Option(name = "KnownSinkName", help = "Name of a known sink (this will be extended)", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> KNOWN_SINK_NAME = new OptionKey<>("nonExistentFunc");

    @Option(name = "Testing", help = "Enable testing taint injector", category = OptionCategory.INTERNAL, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> IS_TESTING = new OptionKey<>(false);
    // @formatter:on

    static private boolean TRACE = Boolean.getBoolean("ttracker.debug.trace");

    public static final String ID = "tainttracker";

    /**
     * Each instrument must override the {@link TruffleInstrument#onCreate(com.oracle.truffle.api.instrumentation.TruffleInstrument.Env)}
     * method.
     * <p>
     * This method is used to properly initialize the instrument. A common practice is to use the {@link Option} system
     * to enable and configure the instrument, as is done in this method. Defining {@link Option}s as is shown in {@link
     * #ENABLED} and , and their usage can be seen in the SimpleCoverageInstrumentTest when the
     * context is being created. Using them from the command line is shown in the simpletool.sh script.
     *
     * @param env the environment for the instrument. Allows us to read the {@link Option}s, input and output streams to
     *            be used for reading and writing, as well as {@link Env#registerService(java.lang.Object) registering}
     *            and {@link Env#lookup(com.oracle.truffle.api.InstrumentInfo, java.lang.Class) looking up} services.
     */
    @Override
    protected void onCreate(final Env env) {
        final OptionValues options = env.getOptions();
        if (ENABLED.getValue(options)) {
            enable(env);
            env.registerService(this);
        }
    }

    public static void trace(String message, Object... parameters) {
        if (TRACE) {
            PrintStream out = System.out;
            out.println("TaintTracker: " + String.format(message, parameters));
        }
    }

    /**
     * Enable the instrument.
     * <p>
     * In this method we enable and configure the instrument. We do this by first creating a {@link SourceSectionFilter}
     * instance in order to specify exactly which parts of the source code we are interested in. In this particular
     * case, we are interested in expressions. Since Truffle Instruments are language agnostic, they rely on language
     * implementers to tag AST nodes with adequate tags. This, we tell our {@link SourceSectionFilter.Builder} that we
     * are care about AST nodes {@link SourceSectionFilter.Builder#tagIs(java.lang.Class...) tagged} with {@link
     * ExpressionTag}. We also tell it we don't care about AST nodes {@link SourceSectionFilter.Builder#includeInternal(boolean)
     * internal} to languages.
     * <p>
     * After than, we use the {@link Env enviroment} to obtain the {@link Instrumenter}, which allows us to specify in
     * which way we wish to instrument the AST.
     * <p>
     * Firstly, we {@link Instrumenter#attachLoadSourceListener(com.oracle.truffle.api.instrumentation.SourceFilter,
     * com.oracle.truffle.api.instrumentation.LoadSourceListener, boolean) attach attach} our own to loading source
     * section events. Each the a {@link SourceSection} is loaded, our listener is notified, so our instrument is always
     * aware of all loaded code. Note that we have specified the filter defined earlier as a constraint, so we are not
     * notified if internal code is loaded.
     * <p>
     * Secondly, we {@link Instrumenter#attachExecutionEventFactory(com.oracle.truffle.api.instrumentation.SourceSectionFilter,
     * com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory) attach} our using the same filter. This factory
     * produces {@link Node Truffle Nodes} that will be inserted into the AST at positions specified by the filter. Each
     * of the inserted nodes will, once executed, remove the corresponding source section from the .
     *
     * @param env The environment, used to get the {@link Instrumenter}
     */
    private void enable(final Env env) {
        SourceSectionFilter inputFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.ExpressionTag.class, JSTags.InputNodeTag.class).build();
        Instrumenter instrumenter = env.getInstrumenter();

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.UnaryOperationTag.class).build(),
                inputFilter,
                ctx -> new UnaryOperationPropagator(TaintTrackerInstrument.this));

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.BinaryOperationTag.class).build(),
                inputFilter,
                ctx -> new BinaryOperationPropagator(TaintTrackerInstrument.this));

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.ReadPropertyTag.class).build(),
                inputFilter,
                ctx -> new PropReadPropagator(TaintTrackerInstrument.this, ctx));

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.FunctionCallTag.class).build(),
                inputFilter,
                ctx -> new RequireEndpoint(TaintTrackerInstrument.this, ctx, LIBRARY_ROOT_DIR.getValue(env.getOptions())));

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.FunctionCallTag.class).build(),
                inputFilter,
                ctx -> new KnownSinkEndpoint(TaintTrackerInstrument.this, new HashSet<>(Arrays.asList(
                        KNOWN_SINK_NAME.getValue(env.getOptions())
                ))));

        instrumenter.attachExecutionEventFactory(
                SourceSectionFilter.newBuilder().tagIs(JSTags.FunctionCallTag.class).build(),
                inputFilter,
                ctx -> new EntryPointTaintInjectorEndpoint(TaintTrackerInstrument.this));


        if (IS_TESTING.getValue(env.getOptions())) {
            // Inject taint on return values of specific func, and log taint value of arguments
            instrumenter.attachExecutionEventFactory(
                    SourceSectionFilter.newBuilder().tagIs(JSTags.FunctionCallTag.class).build(),
                    inputFilter,
                    ctx -> new TestTaintInjectorEndpoint(TaintTrackerInstrument.this, ctx));
        }
    }

    @Override
    protected void onDispose(Env env) {
        System.out.printf("Disposing instrument. Results!\n");
        System.out.printf("Total offenses count: %d\n", offendingTaints.size());
        offendingTaints.stream().forEach(offense -> {
                    System.out.printf("Offense\n", offendingTaints.size());
                    offense.getOrigins().stream().forEach(origin ->
                            System.out.printf("Origin: %s\n", origin));
                }
        );
    }

    /**
     * Which {@link OptionDescriptors} are used for this instrument.
     * <p>
     * If the {@link TruffleInstrument} uses {@link Option}s, it is nesesery to specify which {@link Option}s. The
     * {@link OptionDescriptors} is automatically generated from this class due to the {@link Option} annotation. In our
     * case, this is the {@code SimpleCodeCoverageInstrumentOptionDescriptors} class.
     *
     * @return The class generated by the {@link Option.Group} annotation
     */
    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new TaintTrackerInstrumentOptionDescriptors();
    }

    public int getTaintedCount() {
        return metaStore.getTaintedCount();
    }

    public void taint(Object taintee, String origin) {
        // Both `undefined` and `null` inherit from Nullish
        if (taintee instanceof Nullish) return;
        metaStore.store(taintee, new TaintWithOrigin(origin));
    }

    public void propagateTaint(Object taintee, TaintWithOrigin... taints) {
        // Both `undefined` and `null` inherit from Nullish
        if (taintee instanceof Nullish) return;
        if (taints.length == 1) {
            metaStore.store(taintee, taints[0]);
        } else {
            TaintWithOrigin mergedTaints = Arrays.stream(taints).reduce(TaintWithOrigin.NoTaint.getInstance(), TaintWithOrigin::merge);
            metaStore.store(taintee, mergedTaints);
        }
    }

    public TaintWithOrigin getTaint(Object taintee) {
        // Both `undefined` and `null` inherit from Nullish
        if (taintee instanceof Nullish) return TaintWithOrigin.NoTaint.getInstance();
        return metaStore.retrieve(taintee);
    }

    public void registerOffense(TaintWithOrigin taint) {
        offendingTaints.add(taint);
    }

    public Integer getViolationCount() {
        return offendingTaints.size();
    }

    public Set<String> getViolationOrigins() {
        Set<String> collectedOrigins = new HashSet<>();
        offendingTaints.stream().forEach(two -> collectedOrigins.addAll(two.getOrigins()));
        return collectedOrigins;
    }

    public void registerEntryPoint(JSFunctionObject func, String name) {
        entryPoints.put(func, name);
    }

    public String getEntryPointNameOrNull(JSFunctionObject func) {
        return entryPoints.get(func);
    }
}
