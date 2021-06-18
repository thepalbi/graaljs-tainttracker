package com.oracle.truffle.st.test;

import com.oracle.truffle.st.TaintTrackerInstrument;
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

import static com.oracle.truffle.st.test.TestUtils.readResourceAsString;

@RunWith(Parameterized.class)
public class PropagatorsTest {

    public static final String JS = "js";
    private String sourceResource;
    private int expectedTaints;
    private String label;

    public PropagatorsTest(String sourceResource, Integer expectedTaints, String label) {
        this.sourceResource = sourceResource;
        this.expectedTaints = expectedTaints;
        this.label = label;
    }

    @Parameterized.Parameters(name = "Propagation through {2}")
    public static Collection data() {
        return Arrays.asList(
                new Object[]{"propagation/binary-ops.js", 3, "binary ops"},
                new Object[]{"propagation/prop-read.js", 2, "prop read"});
    }

    @Test
    public void testPropagationWithExpectedTaintCount() throws IOException {
        String souceToExplore = readResourceAsString(getClass().getClassLoader().getResourceAsStream(sourceResource));
        Assume.assumeTrue(Engine.create().getLanguages().containsKey(JS));
        // This is how we can create a context with our tool enabled if we are embeddined in java
        try (Context context = Context.newBuilder(JS).option(TaintTrackerInstrument.ID, "true").build()) {
            Source source = Source.newBuilder(JS, souceToExplore, "test main").build();
            context.eval(source);

            TaintTrackerInstrument instrument = context.getEngine().getInstruments().get(TaintTrackerInstrument.ID).lookup(TaintTrackerInstrument.class);
            Assert.assertEquals(expectedTaints, instrument.getTaintedCount());
        }
    }
}
