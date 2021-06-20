package com.thepalbi.taint.test;

import com.thepalbi.taint.TaintTrackerInstrument;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static com.thepalbi.taint.test.TestUtils.readResourceAsString;

@RunWith(Parameterized.class)
public class PropagatorsTest {

    public static final String JS = "js";
    private String sourceResource;
    private int expectedTaints;
    private final Integer expectedViolations;
    private String label;

    public PropagatorsTest(String sourceResource, Integer expectedTaints, Integer expectedViolations, String label) {
        this.sourceResource = sourceResource;
        this.expectedTaints = expectedTaints;
        this.expectedViolations = expectedViolations;
        this.label = label;
    }

    @Parameterized.Parameters(name = "Propagation through {3}")
    public static Collection data() {
        return Arrays.asList(
                new Object[]{"propagation/unary-ops.js", 2, 1, "unary ops"},
                new Object[]{"propagation/unary-ops-with-null.js", 0, 0, "unary ops, operating on null object"},
                new Object[]{"propagation/unary-ops-with-undefined.js", 0, 0, "unary ops, operating on undefined object"},
                new Object[]{"propagation/binary-ops.js", 2, 1, "binary ops"},
                new Object[]{"propagation/prop-read.js", 2, 1, "prop read, base is tainted"},
                new Object[]{"propagation/prop-read-2.js", 1, 1, "prop read, member is tainted"});
    }

    @Test
    public void testPropagationWithExpectedTaintCount() throws IOException {
        String souceToExplore = readResourceAsString(getClass().getClassLoader().getResourceAsStream(sourceResource));
        Assume.assumeTrue(Engine.create().getLanguages().containsKey(JS));
        // This is how we can create a context with our tool enabled if we are embeddined in java
        try (Context context = Context.newBuilder(JS)
                .option(TaintTrackerInstrument.ID, "true")
                .option("tainttracker.KnownSinkName", "log")
                .option("tainttracker.Testing", "true")
                .build()) {
            Source source = Source.newBuilder(JS, souceToExplore, "test main").build();
            context.eval(source);

            TaintTrackerInstrument instrument = context.getEngine().getInstruments().get(TaintTrackerInstrument.ID).lookup(TaintTrackerInstrument.class);
            Assert.assertEquals("Expect taint counts to be equal", expectedTaints, instrument.getTaintedCount());
            Assert.assertEquals("Expect violation count to be equal", expectedViolations, instrument.getViolationCount());
        }
    }
}
