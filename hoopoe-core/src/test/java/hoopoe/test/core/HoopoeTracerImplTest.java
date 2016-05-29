package hoopoe.test.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiler;
import hoopoe.core.HoopoeTracerImpl;
import static hoopoe.test.core.supplements.HoopoeTestHelper.msToNs;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

public class HoopoeTracerImplTest {

    private HoopoeTracerImpl tracer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HoopoeProfiler profilerMock;

    @Before
    public void prepareTest() {
        MockitoAnnotations.initMocks(this);
        tracer = new HoopoeTracerImpl();
        tracer.setupProfiler(profilerMock);
    }

    @Test
    public void testSingleMethodCall() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            tracer.onMethodEnter("cr", "mr");
            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());

            assertInvocation(actualRoot, "cr", "mr", 1);
            assertNoAttributes(actualRoot);
            assertNoChildren(actualRoot);
        });
    }

    @Test
    public void testAttributesPropagation() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            tracer.onMethodEnter("cr", "mr");
            HoopoeAttribute attribute = new HoopoeAttribute("a1", null, true);
            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.singleton(attribute));

            assertInvocation(actualRoot, "cr", "mr", 1);
            assertThat(actualRoot.getAttributes().size(), equalTo(1));
            assertThat(actualRoot.getAttributes().iterator().next(), equalTo(attribute));
        });
    }

    @Test
    public void testMethodCallHierarchy() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            tracer.onMethodEnter("c1", "m1");
            tracer.onMethodEnter("c2", "m2");
            tracer.onMethodEnter("c3", "m3");

            // pop c3.m3
            HoopoeProfiledInvocation actualInvocation = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualInvocation, nullValue());

            // pop c2.m2
            actualInvocation = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualInvocation, nullValue());

            // pop c1.m1 - root of invocation
            actualInvocation = tracer.onMethodLeave(Collections.emptyList());
            assertInvocation(actualInvocation, "c1", "m1", 1);
            assertChildrenCount(actualInvocation, 1);

            actualInvocation = actualInvocation.getChildren().iterator().next();
            assertInvocation(actualInvocation, "c2", "m2", 1);
            assertChildrenCount(actualInvocation, 1);

            actualInvocation = actualInvocation.getChildren().iterator().next();
            assertInvocation(actualInvocation, "c3", "m3", 1);
            assertNoChildren(actualInvocation);
        });
    }

    @Test
    public void testSortByTotalTime() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);
            tracer.onMethodEnter("cr", "mr");

            // first invocation is fast
            tracer.onMethodEnter("c2", "m2.fast");
            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualRoot, nullValue());

            // second invocation is slow
            tracer.onMethodEnter("c2", "m2.slow");
            Thread.sleep(10);
            actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualRoot, nullValue());

            // pop c1.m1 - root of invocation
            actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertInvocation(actualRoot, "cr", "mr", 1);
            assertChildrenCount(actualRoot, 2);
            Iterator<HoopoeProfiledInvocation> invocationIterator = actualRoot.getChildren().iterator();

            // slower invocation should go first, although was called after the faster one
            HoopoeProfiledInvocation actualSlowInvocation = invocationIterator.next();
            assertInvocation(actualSlowInvocation, "c2", "m2.slow", 1);
            assertNoChildren(actualSlowInvocation);

            HoopoeProfiledInvocation actualFastInvocation = invocationIterator.next();
            assertInvocation(actualFastInvocation, "c2", "m2.fast", 1);
            assertNoChildren(actualFastInvocation);
        });
    }

    /**
     * We cannot test exact time calculations.
     * Thus at least covering case that parent's total time should be greater or equal to eny child's total time
     */
    @Test
    public void testTimeCalculations() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            tracer.onMethodEnter("c1", "m1");
            tracer.onMethodEnter("c2", "m2");
            tracer.onMethodEnter("c3", "m3");

            // pop c3.m3
            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualRoot, nullValue());

            // pop c2.m2
            actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertThat(actualRoot, nullValue());

            // pop c1.m1 - root of invocation
            actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertInvocation(actualRoot, "c1", "m1", 1);
            assertThat(actualRoot.getChildren().size(), equalTo(1));

            HoopoeProfiledInvocation actualC2Invocation = actualRoot.getChildren().iterator().next();
            assertInvocation(actualC2Invocation, "c2", "m2", 1);
            assertChildrenCount(actualC2Invocation, 1);
            assertThat(actualC2Invocation.getTotalTimeInNs(), lessThanOrEqualTo(actualRoot.getTotalTimeInNs()));

            HoopoeProfiledInvocation actualC3Invocation = actualC2Invocation.getChildren().iterator().next();
            assertInvocation(actualC3Invocation, "c3", "m3", 1);
            assertNoChildren(actualC3Invocation);
            assertThat(actualC3Invocation.getTotalTimeInNs(), lessThanOrEqualTo(actualC2Invocation.getTotalTimeInNs()));
        });
    }

    @Test
    public void testBaseMergeCase() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            // root (cr.mr) -> n1 (c1.m1) -> n2 (c.m) -> n3 (c3.m3)
            //                            -> n4 (c.m)

            tracer.onMethodEnter("cr", "mr");
            tracer.onMethodEnter("c1", "m1");

            tracer.onMethodEnter("c", "m");

            tracer.onMethodEnter("c3", "m3");
            tracer.onMethodLeave(Collections.emptyList());

            // leave n2, pop n1
            tracer.onMethodLeave(Collections.emptyList());

            // visit n4
            tracer.onMethodEnter("c", "m");
            tracer.onMethodLeave(Collections.emptyList());

            // pop n1, pop root
            tracer.onMethodLeave(Collections.emptyList());
            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());

            assertThat(actualRoot, notNullValue());

            assertInvocation(actualRoot, "cr", "mr", 1);
            assertChildrenCount(actualRoot, 1);

            HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().iterator().next();
            assertInvocation(actualN1, "c1", "m1", 1);
            assertChildrenCount(actualN1, 1);

            HoopoeProfiledInvocation actualMergedInvocation = actualN1.getChildren().iterator().next();
            // importantly, invocations count is 2
            assertInvocation(actualMergedInvocation, "c", "m", 2);
            assertChildrenCount(actualMergedInvocation, 1);

            HoopoeProfiledInvocation actualN3 = actualMergedInvocation.getChildren().iterator().next();
            assertInvocation(actualN3, "c3", "m3", 1);
            assertNoChildren(actualN3);
        });
    }

    @Test
    public void testSameMethodWithDifferentAttributesIsNotMerged() throws Throwable {
        executeTest(() -> {
            mockMinimumTrackedInvocationTimeInNs(0);

            tracer.onMethodEnter("cr", "mr");

            tracer.onMethodEnter("c", "m");
            // increase execution time to have predictable children order
            Thread.sleep(15);
            HoopoeAttribute firstAttribute = new HoopoeAttribute("sql", "query1", true);
            tracer.onMethodLeave(Collections.singletonList(firstAttribute));

            tracer.onMethodEnter("c", "m");
            HoopoeAttribute secondAttribute = new HoopoeAttribute("sql", "query2", true);
            tracer.onMethodLeave(Collections.singletonList(secondAttribute));

            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertInvocation(actualRoot, "cr", "mr", 1);
            assertChildrenCount(actualRoot, 2);
            assertNoAttributes(actualRoot);

            Iterator<HoopoeProfiledInvocation> childrenIterator = actualRoot.getChildren().iterator();

            HoopoeProfiledInvocation actualChild = childrenIterator.next();
            assertInvocation(actualChild, "c", "m", 1);
            assertNoChildren(actualChild);
            assertThat(actualChild.getAttributes().size(), equalTo(1));
            assertThat(actualChild.getAttributes().iterator().next(), equalTo(firstAttribute));

            actualChild = childrenIterator.next();
            assertInvocation(actualChild, "c", "m", 1);
            assertNoChildren(actualChild);
            assertThat(actualChild.getAttributes().size(), equalTo(1));
            assertThat(actualChild.getAttributes().iterator().next(), equalTo(secondAttribute));
        });
    }

    @Test
    public void testFastInvocationsAreTrimmed() throws Throwable {
        executeTest(() -> {
            long minimumTimeToTrackInMs = 10;

            mockMinimumTrackedInvocationTimeInNs(msToNs(minimumTimeToTrackInMs));

            tracer.onMethodEnter("cr", "mr");

            tracer.onMethodEnter("c1", "m1");
            // increase execution time to keep this record
            Thread.sleep(minimumTimeToTrackInMs);

            // too fast, should be trimmed
            tracer.onMethodEnter("c2", "m2");
            tracer.onMethodLeave(Collections.emptyList());

            tracer.onMethodLeave(Collections.emptyList());

            // too fast, should be trimmed
            tracer.onMethodEnter("c3", "m3");
            tracer.onMethodLeave(Collections.emptyList());

            HoopoeProfiledInvocation actualRoot = tracer.onMethodLeave(Collections.emptyList());
            assertInvocation(actualRoot, "cr", "mr", 1);
            assertChildrenCount(actualRoot, 1);

            HoopoeProfiledInvocation actualChild = actualRoot.getChildren().iterator().next();
            assertInvocation(actualChild, "c1", "m1", 1);
            assertNoChildren(actualChild);
        });
    }

    private void assertInvocation(HoopoeProfiledInvocation actualInvocation,
                                  String expectedClassName,
                                  String expectedMethodSignature,
                                  int expectedInvocationsCount) {
        assertThat(actualInvocation, notNullValue());
        assertThat(actualInvocation.getClassName(), equalTo(expectedClassName));
        assertThat(actualInvocation.getMethodSignature(), equalTo(expectedMethodSignature));
        assertThat(actualInvocation.getChildren(), notNullValue());
        assertThat(actualInvocation.getAttributes(), notNullValue());
        assertThat(actualInvocation.getTotalTimeInNs(), greaterThanOrEqualTo(actualInvocation.getOwnTimeInNs()));
        assertThat(actualInvocation.getInvocationsCount(), equalTo(expectedInvocationsCount));
    }

    private void assertNoChildren(HoopoeProfiledInvocation actualInvocation) {
        assertChildrenCount(actualInvocation, 0);
    }

    private void assertNoAttributes(HoopoeProfiledInvocation actualInvocation) {
        assertThat(actualInvocation.getAttributes().size(), equalTo(0));
    }

    private void assertChildrenCount(HoopoeProfiledInvocation actualInvocation, int expectedChildrenCount) {
        assertThat(actualInvocation.getChildren().size(), equalTo(expectedChildrenCount));
    }

    private void mockMinimumTrackedInvocationTimeInNs(long value) {
        when(profilerMock.getConfiguration().getMinimumTrackedInvocationTimeInNs()).thenReturn(value);
    }

    /**
     * Executes every test in separate thread, to avoid implementation thread local clashes between tests
     */
    //todo make it cleaner, with @Rule ?
    private void executeTest(RunnableWithException testCode) throws Throwable {
        AtomicReference<Throwable> exceptionReference = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                testCode.run();
            }
            catch (Throwable e) {
                exceptionReference.set(e);
            }
        });
        thread.start();
        thread.join();

        Throwable exception = exceptionReference.get();
        if (exception != null) {
            throw exception;
        }
    }

    private interface RunnableWithException {
        void run() throws Exception;
    }

}
