package hoopoe.core.tracer;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationRoot;
import hoopoe.api.HoopoeProfiledResult;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TraceNormalizerTest {

    private TraceNormalizer traceNormalizer = new TraceNormalizer();

    @Test
    public void testEmptyInput() {
        HoopoeProfiledResult profiledResult = traceNormalizer.calculateProfiledResult(Collections.emptyList());

        assertThat("Profiled Result must not be null", profiledResult, notNullValue());
        assertThat("Profiled Result invocations must not be null", profiledResult.getInvocations(), notNullValue());
        assertTrue("Profiled Result must empty", profiledResult.getInvocations().isEmpty());
    }

    @Test
    public void testParentNodeCalculation() {
        ThreadTracer threadTracer = new ThreadTracer();
        List<TraceNode> nodes = threadTracer.init();
        nodes.add(new TraceNode("child", "m1", 42, 45, null));
        nodes.add(new TraceNode("parent", "m2", 40, 45, null));
        nodes.add(new TraceNode("anotherRoot", "m3", 46, 50, null));

        HoopoeProfiledResult profiledResult = traceNormalizer.calculateProfiledResult(
                Collections.singleton(threadTracer));

        assertThat("Profiled Result must not be null", profiledResult, notNullValue());
        Collection<HoopoeProfiledInvocationRoot> invocations = profiledResult.getInvocations();
        assertThat("Profiled Result invocations must not be null", invocations, notNullValue());
        assertThat("Profiled Result should have 2 roots", invocations, hasSize(2));

        for (HoopoeProfiledInvocationRoot root : invocations) {
            String rootClassName = root.getInvocation().getClassName();
            if ("parent".equals(rootClassName)) {
                assertThat("Parent should have one single child",
                        root.getInvocation().getChildren(), hasSize(1));

                HoopoeProfiledInvocation child = root.getInvocation().getChildren().get(0);
                assertThat("Proper node should be set as child", child.getClassName(), equalTo("child"));

            } else if ("anotherRoot".equals(rootClassName)) {
                assertThat("No child invocations should be registered for another root",
                        root.getInvocation().getChildren(), empty());

            } else {
                fail("Unexpected root class name" + rootClassName);
            }
        }
    }

}