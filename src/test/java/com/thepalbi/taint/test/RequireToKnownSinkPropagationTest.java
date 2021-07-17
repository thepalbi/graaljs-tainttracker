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
import java.util.List;

import static com.thepalbi.taint.test.PropagatorsTest.JS;
import static com.thepalbi.taint.test.TestUtils.readResourceAsString;
import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class RequireToKnownSinkPropagationTest {

    private String sourceResource;
    private Integer expectedOffenses;
    private String label;
    private final List<String> expectedOrigins;

    public RequireToKnownSinkPropagationTest(String sourceResource, int expectedOffenses, String label, List<String> expectedOrigins) {
        this.sourceResource = sourceResource;
        this.expectedOffenses = expectedOffenses;
        this.label = label;
        this.expectedOrigins = expectedOrigins;
    }


    @Parameterized.Parameters(name = "{2}")
    public static Collection data() {
        return asList(
                new Object[]{"e2e/simple.js", 1, "simple propagation", asList("entry")},
                new Object[]{"e2e/compound.js", 2, "simple propagation with compound return", asList("entryFunc", "entryFunc2")},
                new Object[]{"e2e/with-wrapper-obj.js", 1, "propagation across wrapped object", asList("entryFunc")},
                new Object[]{"e2e/simple-and-wrapped.js", 2, "propagation with simple and wrapped object", asList("wrappedEntryFunc", "simpleEntryFunc")}
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
            for (String expectedOrigin : expectedOrigins) {
                Assert.assertTrue(instrument.getViolationOrigins().contains(expectedOrigin));
            }
        }
    }


}
