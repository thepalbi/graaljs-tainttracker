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
import java.util.Collections;

import static com.thepalbi.taint.test.PropagatorsTest.JS;
import static com.thepalbi.taint.test.TestUtils.readResourceAsString;

@RunWith(Parameterized.class)
public class RequireToKnownSinkPropagationTest {

    private String sourceResource;
    private Integer expectedOffenses;
    private String label;

    public RequireToKnownSinkPropagationTest(String sourceResource, int expectedOffenses, String label) {
        this.sourceResource = sourceResource;
        this.expectedOffenses = expectedOffenses;
        this.label = label;
    }


    @Parameterized.Parameters(name = "{1}")
    public static Collection data() {
        return Collections.singletonList(
                new Object[]{"e2e/simple.js", 1, "simple propagation"}
        );
    }

    @Test
    public void testTaintFromRequiresIsPropagatedToKnownSinks() throws IOException {
        String souceCode = readResourceAsString(getClass().getClassLoader().getResourceAsStream(sourceResource));
        Assume.assumeTrue(Engine.create().getLanguages().containsKey(JS));
        // This is how we can create a context with our tool enabled if we are embeddined in java
        try (Context context = Context.newBuilder(JS)
                .option(TaintTrackerInstrument.ID, "true")
                .option("tainttracker.KnownSinkName", "log")
                .option("tainttracker.LibraryRootDir", System.getProperty("user.dir"))
                .build()) {
            Source source = Source.newBuilder(JS, souceCode, "test main").build();
            context.eval(source);

            TaintTrackerInstrument instrument = context.getEngine().getInstruments().get(TaintTrackerInstrument.ID).lookup(TaintTrackerInstrument.class);
            Assert.assertEquals("Expected the number of offenses to be equal", expectedOffenses, instrument.getViolationCount());
        }
    }


}
