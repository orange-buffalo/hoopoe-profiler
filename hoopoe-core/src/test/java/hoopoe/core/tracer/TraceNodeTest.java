package hoopoe.core.tracer;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.plugins.HoopoeInvocationAttribute;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class TraceNodeTest {

    @Test
    public void testProfilerOverheadCalculations() throws NoSuchFieldException, IllegalAccessException {
        long endTime = System.nanoTime();
        TraceNode node = new TraceNode("c1", "m1", endTime - msToNs(10_000), endTime, null);

        long profileOverhead = getProfileOverhead(node);
        assertThat(profileOverhead, greaterThanOrEqualTo(0L));

        long totalTime = getTotalTime(node);
        assertThat(totalTime, equalTo(msToNs(10_000) - profileOverhead));

        long ownTime = getOwnTime(node);
        assertThat(ownTime, equalTo(totalTime));
    }

    @Test
    public void testSingleMethodCall() throws Throwable {
        TraceNode node = createNode("cr", "mr", 100, 20);
        HoopoeProfiledInvocation actualRoot = node.convertToProfiledInvocation();

        assertInvocation(actualRoot, "cr", "mr");
        assertNoAttributes(actualRoot);
        assertNoChildren(actualRoot);
        assertSingleInvocation(actualRoot);
        assertOwnTimeSameAsTotalTime(actualRoot, 100);
    }

    @Test
    public void testAttributesPropagation() throws Throwable {
        HoopoeInvocationAttribute attribute = HoopoeInvocationAttribute.noTimeContribution("a1", null);
        TraceNode node = createNode("cr", "mr", 100, 0, Collections.singleton(attribute));
        HoopoeProfiledInvocation actualRoot = node.convertToProfiledInvocation();

        assertInvocation(actualRoot, "cr", "mr");
        assertThat(actualRoot.getAttributes().size(), equalTo(1));
        assertThat(actualRoot.getAttributes().iterator().next(), equalTo(attribute));
    }

    @Test
    public void testChildrenTimeCalculations() throws Throwable {
        // c1.m1 -> c2.m2 -> c3.m3
        //       -> c4.m4

        TraceNode n1 = createNode("c1", "m1", 700, 4);
        TraceNode n2 = createNode("c2", "m2", 400, 7);
        TraceNode n3 = createNode("c3", "m3", 30, 1);
        TraceNode n4 = createNode("c4", "m4", 70, 2);

        n2.addChild(n3);
        n1.addChild(n2);
        n1.addChild(n4);

        // no changes for most deep children, as they have no their own children
        assertThat(getOwnTime(n3), equalTo(30L));
        assertThat(getTotalTime(n3), equalTo(30L));
        assertThat(getProfileOverhead(n3), equalTo(1L));

        assertThat(getOwnTime(n4), equalTo(70L));
        assertThat(getTotalTime(n4), equalTo(70L));
        assertThat(getProfileOverhead(n4), equalTo(2L));

        // all nodes with children should accumulate children timing

        // initial own time - n3.totalTime - n3.profilerOverhead
        assertThat(getOwnTime(n2), equalTo(400L - 30 - 1));
        // initial total time - n3.profilerOverhead
        assertThat(getTotalTime(n2), equalTo(400L - 1));
        // initial profiler overhead + n3.profilerOverhead
        assertThat(getProfileOverhead(n2), equalTo(7L + 1));

        // initial own time - n2.totalTime - n2.profilerOverhead - n4.totalTime - n4.profilerOverhead
        assertThat(getOwnTime(n1),
                equalTo(700L - getTotalTime(n2) - getProfileOverhead(n2) - getTotalTime(n4) - getProfileOverhead(n4)));
        // initial total time - n2.profilerOverhead - n4.profilerOverhead
        assertThat(getTotalTime(n1), equalTo(700L - getProfileOverhead(n2) - getProfileOverhead(n4)));
        // initial profile overhead + n2.profilerOverhead + n4.profilerOverhead
        assertThat(getProfileOverhead(n1), equalTo(4L + getProfileOverhead(n2) + getProfileOverhead(n4)));
    }

    @Test
    public void testMethodChainTransformation() throws Throwable {
        // c1.m1 -> c2.m2 -> c3.m3

        TraceNode n1 = createNode("c1", "m1", 700, 4);
        TraceNode n2 = createNode("c2", "m2", 400, 7);
        TraceNode n3 = createNode("c3", "m3", 30, 1);

        n2.addChild(n3);
        n1.addChild(n2);

        HoopoeProfiledInvocation actualN1 = n1.convertToProfiledInvocation();
        assertInvocation(actualN1, "c1", "m1");
        assertNoAttributes(actualN1);
        assertSingleInvocation(actualN1);
        assertTotalTime(actualN1, getTotalTime(n1));
        assertOwnTime(actualN1, getOwnTime(n1));
        assertChildrenCount(actualN1, 1);

        HoopoeProfiledInvocation actualN2 = actualN1.getChildren().get(0);
        assertInvocation(actualN2, "c2", "m2");
        assertNoAttributes(actualN2);
        assertSingleInvocation(actualN2);
        assertTotalTime(actualN2, getTotalTime(n2));
        assertOwnTime(actualN2, getOwnTime(n2));
        assertChildrenCount(actualN2, 1);

        HoopoeProfiledInvocation actualN3 = actualN2.getChildren().get(0);
        assertInvocation(actualN3, "c3", "m3");
        assertNoAttributes(actualN3);
        assertSingleInvocation(actualN3);
        assertTotalTime(actualN3, getTotalTime(n3));
        assertOwnTime(actualN3, getOwnTime(n3));
        assertNoChildren(actualN3);
    }

    @Test
    public void testSortByTotalTime() throws Throwable {
        // cr.mr -> c2.fast
        //       -> c2.slow

        TraceNode root = createNode("cr", "mr", 700, 0);
        TraceNode childFast = createNode("child", "fast", 300, 0);
        TraceNode childSlow = createNode("child", "slow", 301, 0);

        // fast is executed before slow
        root.addChild(childFast);
        root.addChild(childSlow);

        HoopoeProfiledInvocation actualRoot = root.convertToProfiledInvocation();
        assertInvocation(actualRoot, "cr", "mr");
        assertChildrenCount(actualRoot, 2);

        // slow invocation should be first due to sorting rules, although it was executed after fast invocation
        HoopoeProfiledInvocation actualSlow = actualRoot.getChildren().get(0);
        assertInvocation(actualSlow, "child", "slow");
        assertNoChildren(actualSlow);

        HoopoeProfiledInvocation actualFast = actualRoot.getChildren().get(1);
        assertInvocation(actualFast, "child", "fast");
        assertNoChildren(actualFast);
    }

    @Test
    public void testBaseMergeCase() throws Throwable {
        // root (cr.mr) -> n1 (c1.m1) -> n2 (c.m) -> n3 (c3.m3)
        //                            -> n4 (c.m)

        TraceNode n3 = createNode("c3", "m3", 30, 1);

        TraceNode n2 = createNode("c", "m", 300, 2);
        n2.addChild(n3);

        TraceNode n4 = createNode("c", "m", 70, 3);

        TraceNode n1 = createNode("c1", "m1", 900, 5);
        n1.addChild(n4);
        n1.addChild(n2);

        TraceNode root = createNode("cr", "mr", 1000, 9);
        root.addChild(n1);

        HoopoeProfiledInvocation actualRoot = root.convertToProfiledInvocation();
        assertInvocation(actualRoot, "cr", "mr");
        assertSingleInvocation(actualRoot);
        assertTotalTime(actualRoot, 989);
        assertOwnTime(actualRoot, 95);
        assertChildrenCount(actualRoot, 1);

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().iterator().next();
        assertInvocation(actualN1, "c1", "m1");
        assertSingleInvocation(actualN1);
        assertTotalTime(actualN1, 894);
        assertOwnTime(actualN1, 525);
        assertChildrenCount(actualN1, 1);

        HoopoeProfiledInvocation actualMergedInvocation = actualN1.getChildren().iterator().next();
        assertInvocation(actualMergedInvocation, "c", "m");
        // importantly, invocations count is 2
        assertInvocationsCount(actualMergedInvocation, 2);
        // timing should be accumulated
        assertTotalTime(actualMergedInvocation, 369);
        assertOwnTime(actualMergedInvocation, 339);
        // children should be merged
        assertChildrenCount(actualMergedInvocation, 1);

        HoopoeProfiledInvocation actualN3 = actualMergedInvocation.getChildren().iterator().next();
        assertInvocation(actualN3, "c3", "m3");
        assertTotalTime(actualN3, 30);
        assertOwnTime(actualN3, 30);
        assertSingleInvocation(actualN3);
        assertNoChildren(actualN3);
    }

    @Test
    public void testSameMethodWithDifferentAttributesIsNotMerged() throws Throwable {
        TraceNode root = createNode("cr", "mr", 1000, 0);

        HoopoeInvocationAttribute firstAttribute = HoopoeInvocationAttribute.withTimeContribution("sql", "query1");
        root.addChild(
                createNode("c", "m", 800, 0, Collections.singletonList(firstAttribute)));

        HoopoeInvocationAttribute secondAttribute = HoopoeInvocationAttribute.withTimeContribution("sql", "query2");
        root.addChild(
                createNode("c", "m", 20, 0, Collections.singletonList(secondAttribute))
        );

        HoopoeProfiledInvocation actualRoot = root.convertToProfiledInvocation();
        assertInvocation(actualRoot, "cr", "mr");
        assertChildrenCount(actualRoot, 2);
        assertSingleInvocation(actualRoot);
        assertNoAttributes(actualRoot);

        Iterator<HoopoeProfiledInvocation> childrenIterator = actualRoot.getChildren().iterator();

        HoopoeProfiledInvocation actualChild = childrenIterator.next();
        assertInvocation(actualChild, "c", "m");
        assertSingleInvocation(actualChild);
        assertNoChildren(actualChild);
        assertThat(actualChild.getAttributes().size(), equalTo(1));
        assertThat(actualChild.getAttributes().iterator().next(), equalTo(firstAttribute));

        actualChild = childrenIterator.next();
        assertInvocation(actualChild, "c", "m");
        assertSingleInvocation(actualChild);
        assertNoChildren(actualChild);
        assertThat(actualChild.getAttributes().size(), equalTo(1));
        assertThat(actualChild.getAttributes().iterator().next(), equalTo(secondAttribute));
    }

    private void assertInvocation(HoopoeProfiledInvocation actualInvocation,
                                  String expectedClassName,
                                  String expectedMethodSignature) {
        assertThat(actualInvocation, notNullValue());
        assertThat(actualInvocation.getClassName(), equalTo(expectedClassName));
        assertThat(actualInvocation.getMethodSignature(), equalTo(expectedMethodSignature));
        assertThat(actualInvocation.getChildren(), notNullValue());
        assertThat(actualInvocation.getAttributes(), notNullValue());
    }

    private void assertSingleInvocation(HoopoeProfiledInvocation actualInvocation) {
        assertInvocationsCount(actualInvocation, 1);
    }

    private void assertInvocationsCount(HoopoeProfiledInvocation actualInvocation, int expectedInvocationsCount) {
        assertThat(actualInvocation.getInvocationsCount(), equalTo(expectedInvocationsCount));
    }

    private void assertTotalTime(HoopoeProfiledInvocation actualInvocation, long expectedTotalTime) {
        assertThat(actualInvocation.getTotalTimeInNs(), equalTo(expectedTotalTime));
    }

    private void assertOwnTime(HoopoeProfiledInvocation actualInvocation, long expectedOwnTime) {
        assertThat(actualInvocation.getOwnTimeInNs(), equalTo(expectedOwnTime));
    }

    private void assertOwnTimeSameAsTotalTime(HoopoeProfiledInvocation actualInvocation, long expectedTime) {
        assertTotalTime(actualInvocation, expectedTime);
        assertOwnTime(actualInvocation, expectedTime);
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

    private TraceNode createNode(String className, String methodSignature, long totalTime, long overhead)
            throws Exception {
        return createNode(className, methodSignature, totalTime, overhead, null);
    }

    private TraceNode createNode(String className, String methodSignature, long totalTime,
                                 long overhead, Collection<HoopoeInvocationAttribute> attributes) throws Exception {

        TraceNode node = new TraceNode(className, methodSignature, 0, 0, attributes);
        setProfileOverhead(node, overhead);
        setTotalTime(node, totalTime);
        setOwnTime(node, totalTime);
        return node;
    }

    private void setOwnTime(TraceNode node, long time) throws NoSuchFieldException, IllegalAccessException {
        getOwnTimeField().set(node, time);
    }

    private long getOwnTime(TraceNode node) throws NoSuchFieldException, IllegalAccessException {
        return (long) getOwnTimeField().get(node);
    }

    private Field getOwnTimeField() throws NoSuchFieldException {
        return getField("ownTimeInNs");
    }

    private void setProfileOverhead(TraceNode node, long overhead) throws NoSuchFieldException, IllegalAccessException {
        getProfileOverheadField().set(node, overhead);
    }

    private long getProfileOverhead(TraceNode node) throws NoSuchFieldException, IllegalAccessException {
        return (long) getProfileOverheadField().get(node);
    }

    private Field getProfileOverheadField() throws NoSuchFieldException {
        return getField("profilerOverheadInNs");
    }

    private void setTotalTime(TraceNode node, long time) throws NoSuchFieldException, IllegalAccessException {
        getTotalTimeField().set(node, time);
    }

    private long getTotalTime(TraceNode node) throws NoSuchFieldException, IllegalAccessException {
        return (long) getTotalTimeField().get(node);
    }

    private Field getTotalTimeField() throws NoSuchFieldException {
        return getField("totalTimeInNs");
    }

    private Field getField(String name) throws NoSuchFieldException {
        Field field = TraceNode.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static long msToNs(long ms) {
        return ms * 1_000_000;
    }

}
