package hoopoe.test.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeAttributeSummary;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationSummary;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.core.HoopoeStorageImpl;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class StorageTest {

    private static final String THREAD_NAME = "threadName";

    private HoopoeStorageImpl storage;

    private Thread thread;

    @Before
    public void prepareTest() {
        storage = new HoopoeStorageImpl();

        thread = new Thread();
        thread.setName(THREAD_NAME);
    }

    @Test
    public void testSingleProfiledInvocation() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        storage.consumeThreadTraceResults(thread, root);
        storage.waitForProvisioning();

        Collection<HoopoeProfiledInvocationSummary> actualSummaries = storage.getProfiledInvocationSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(1));

        HoopoeProfiledInvocationSummary actualSummary = actualSummaries.iterator().next();
        assertThat(actualSummary.getThreadName(), equalTo(THREAD_NAME));
        assertThat(actualSummary.getProfiledOn(), notNullValue());
        assertThat(actualSummary.getId(), notNullValue());

        HoopoeProfiledInvocation actualInvocation = storage.getProfiledInvocation(actualSummary.getId());
        assertNotMergedInvocation(actualInvocation, root);
    }

    @Test
    public void testMultipleProfiledInvocations() throws Exception {
        HoopoeTraceNode root1 = new HoopoeTraceNode(null, "c1", "m1");
        storage.consumeThreadTraceResults(thread, root1);

        HoopoeTraceNode root2 = new HoopoeTraceNode(null, "c2", "m2");
        storage.consumeThreadTraceResults(thread, root2);

        storage.waitForProvisioning();

        Collection<HoopoeProfiledInvocationSummary> actualSummaries = storage.getProfiledInvocationSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(2));

        Iterator<HoopoeProfiledInvocationSummary> actualSummariesIterator = actualSummaries.iterator();
        HoopoeProfiledInvocationSummary actualSummary = actualSummariesIterator.next();
        assertThat(actualSummary.getThreadName(), equalTo(THREAD_NAME));
        assertThat(actualSummary.getProfiledOn(), notNullValue());
        assertThat(actualSummary.getId(), notNullValue());

        HoopoeProfiledInvocation actualInvocation = storage.getProfiledInvocation(actualSummary.getId());
        assertNotMergedInvocation(actualInvocation, root1);

        actualSummary = actualSummariesIterator.next();
        assertThat(actualSummary.getThreadName(), equalTo(THREAD_NAME));
        assertThat(actualSummary.getProfiledOn(), notNullValue());
        assertThat(actualSummary.getId(), notNullValue());

        actualInvocation = storage.getProfiledInvocation(actualSummary.getId());
        assertNotMergedInvocation(actualInvocation, root2);
    }

    @Test
    public void testMethodCallHierarchy() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 0, 1);

        HoopoeTraceNode n2 = new HoopoeTraceNode(n1, "c2", "m2");
        setTimeInMs(n2, 1, 100);

        // this child execution is slower, it should be the first in profiled results
        HoopoeTraceNode n3 = new HoopoeTraceNode(n1, "c3", "m3");
        setTimeInMs(n3, 150, 500);

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertNotMergedInvocation(actualRoot, root);

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertNotMergedInvocation(actualN1, n1);

        // remember, 2.2 is slower than 2.1 and should be in the list before
        HoopoeProfiledInvocation actualN3 = actualN1.getChildren().get(0);
        assertNotMergedInvocation(actualN3, n3);

        HoopoeProfiledInvocation actualN2 = actualN1.getChildren().get(1);
        assertNotMergedInvocation(actualN2, n2);
    }

    @Test
    public void testTimeCalculations() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 5, 80);

        HoopoeTraceNode n2 = new HoopoeTraceNode(n1, "c2", "m2");
        setTimeInMs(n2, 6, 30);

        HoopoeTraceNode n3 = new HoopoeTraceNode(n1, "c3", "m3");
        setTimeInMs(n3, 40, 55);

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertThat(actualRoot.getTotalTimeInNs(), equalTo(msToNs(100)));
        assertThat(actualRoot.getOwnTimeInNs(), equalTo(msToNs(100 - (80 - 5))));

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertThat(actualN1.getTotalTimeInNs(), equalTo(msToNs(80 - 5)));
        assertThat(actualN1.getOwnTimeInNs(), equalTo(msToNs(80 - 5 - ((30 - 6) + (55 - 40)))));

        HoopoeProfiledInvocation actualN2 = actualN1.getChildren().get(0);
        assertThat(actualN2.getTotalTimeInNs(), equalTo(msToNs(30 - 6)));
        assertThat(actualN2.getOwnTimeInNs(), equalTo(msToNs(30 - 6)));

        HoopoeProfiledInvocation actualN3 = actualN1.getChildren().get(1);
        assertThat(actualN3.getTotalTimeInNs(), equalTo(msToNs(55 - 40)));
        assertThat(actualN3.getOwnTimeInNs(), equalTo(msToNs(55 - 40)));
    }

    @Test
    public void testDifferentAttributeSummaries() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n2, 85, 90);
        n2.addAttribute(new HoopoeAttribute("transaction", null, false));

        HoopoeProfiledInvocationSummary actualInvocationSummary = executeAndGetSummary(root);
        Collection<HoopoeAttributeSummary> actualSummaries = actualInvocationSummary.getAttributeSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(2));

        Iterator<HoopoeAttributeSummary> summaryIterator = actualSummaries.iterator();
        HoopoeAttributeSummary actualAttributeSummary = summaryIterator.next();
        assertThat(actualAttributeSummary.getName(), equalTo("sql"));
        assertThat(actualAttributeSummary.getDetails(), equalTo("query"));
        assertThat(actualAttributeSummary.isContributingTime(), equalTo(true));
        assertThat(actualAttributeSummary.getTotalOccurrences(), equalTo(1));
        assertThat(actualAttributeSummary.getTotalTimeInNs(), equalTo(msToNs(80 - 5)));

        actualAttributeSummary = summaryIterator.next();
        assertThat(actualAttributeSummary.getName(), equalTo("transaction"));
        assertThat(actualAttributeSummary.getDetails(), nullValue());
        assertThat(actualAttributeSummary.isContributingTime(), equalTo(false));
        assertThat(actualAttributeSummary.getTotalOccurrences(), equalTo(1));
        assertThat(actualAttributeSummary.getTotalTimeInNs(), equalTo(msToNs(90 - 85)));
    }

    @Test
    public void testDifferentAttributeSummariesDetails() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query1", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n2, 85, 90);
        n2.addAttribute(new HoopoeAttribute("sql", "query2", true));

        HoopoeProfiledInvocationSummary actualInvocationSummary = executeAndGetSummary(root);
        Collection<HoopoeAttributeSummary> actualSummaries = actualInvocationSummary.getAttributeSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(2));

        Iterator<HoopoeAttributeSummary> summaryIterator = actualSummaries.iterator();
        HoopoeAttributeSummary actualAttributeSummary = summaryIterator.next();
        assertThat(actualAttributeSummary.getName(), equalTo("sql"));
        assertThat(actualAttributeSummary.getDetails(), equalTo("query1"));
        assertThat(actualAttributeSummary.isContributingTime(), equalTo(true));
        assertThat(actualAttributeSummary.getTotalOccurrences(), equalTo(1));
        assertThat(actualAttributeSummary.getTotalTimeInNs(), equalTo(msToNs(80 - 5)));

        actualAttributeSummary = summaryIterator.next();
        assertThat(actualAttributeSummary.getName(), equalTo("sql"));
        assertThat(actualAttributeSummary.getDetails(), equalTo("query2"));
        assertThat(actualAttributeSummary.isContributingTime(), equalTo(true));
        assertThat(actualAttributeSummary.getTotalOccurrences(), equalTo(1));
        assertThat(actualAttributeSummary.getTotalTimeInNs(), equalTo(msToNs(90 - 85)));
    }

    @Test
    public void testAttributeSummariesMerge() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n2, 85, 90);
        n2.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeProfiledInvocationSummary actualInvocationSummary = executeAndGetSummary(root);
        Collection<HoopoeAttributeSummary> actualSummaries = actualInvocationSummary.getAttributeSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(1));

        HoopoeAttributeSummary actualAttributeSummary = actualSummaries.iterator().next();
        assertThat(actualAttributeSummary.getName(), equalTo("sql"));
        assertThat(actualAttributeSummary.getDetails(), equalTo("query"));
        assertThat(actualAttributeSummary.isContributingTime(), equalTo(true));
        assertThat(actualAttributeSummary.getTotalOccurrences(), equalTo(2));
        assertThat(actualAttributeSummary.getTotalTimeInNs(), equalTo(msToNs((80 - 5) + (90 - 85))));
    }

    @Test
    public void testSameAttributeSummaries() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n2, 85, 90);
        n2.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeProfiledInvocationSummary actualInvocationSummary = executeAndGetSummary(root);
        Collection<HoopoeAttributeSummary> attributeSummaries = actualInvocationSummary.getAttributeSummaries();
        assertThat(attributeSummaries, notNullValue());
        assertThat(attributeSummaries.size(), equalTo(1));
        HoopoeAttributeSummary attributeSummary = attributeSummaries.iterator().next();
        assertThat(attributeSummary.getTotalOccurrences(), equalTo(2));
        assertThat(attributeSummary.getName(), equalTo("sql"));
        assertThat(attributeSummary.getDetails(), equalTo("query"));
        assertThat(attributeSummary.getTotalTimeInNs(), equalTo(msToNs((80 - 5) + (90 - 85))));
    }

    @Test
    public void testBaseMergeCase() throws Exception {
        // root (cr.mr) -> n1 (c1.m1) -> n2 (c.m) -> n3 (c3.m3)
        //                            -> n4 (c.m)

        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 500);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 10, 400);

        HoopoeTraceNode n2 = new HoopoeTraceNode(n1, "c", "m");
        setTimeInMs(n2, 20, 300);

        HoopoeTraceNode n3 = new HoopoeTraceNode(n2, "c3", "m3");
        setTimeInMs(n3, 30, 40);

        HoopoeTraceNode n4 = new HoopoeTraceNode(n1, "c", "m");
        setTimeInMs(n4, 320, 350);

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertNotMergedInvocation(actualRoot, root);

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertInvocation(actualN1, n1);
        assertThat(actualN1.getChildren().size(), equalTo(1));
        assertThat(actualN1.getInvocationsCount(), equalTo(1));

        HoopoeProfiledInvocation actualMergedInvocation = actualN1.getChildren().get(0);
        assertInvocation(actualMergedInvocation, n2);
        assertThat(actualMergedInvocation.getChildren().size(), equalTo(1));
        assertThat(actualMergedInvocation.getInvocationsCount(), equalTo(2));
        assertThat(actualMergedInvocation.getTotalTimeInNs(), equalTo(msToNs((300 - 20) + (350 - 320))));
        assertThat(actualMergedInvocation.getOwnTimeInNs(), equalTo(msToNs((300 - 20) + (350 - 320) - (40 - 30))));

        HoopoeProfiledInvocation actualN3 = actualMergedInvocation.getChildren().get(0);
        assertNotMergedInvocation(actualN3, n3);
    }

    @Test
    public void testMergeSameMethodWithDifferentAttributes() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 500);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c", "m");
        setTimeInMs(n1, 10, 300);
        n1.addAttribute(new HoopoeAttribute("sql", "query1", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c", "m");
        setTimeInMs(n2, 320, 400);
        n2.addAttribute(new HoopoeAttribute("sql", "query2", true));

        HoopoeTraceNode n3 = new HoopoeTraceNode(root, "c", "m");
        setTimeInMs(n3, 410, 412);

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertNotMergedInvocation(actualRoot, root);

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertNotMergedInvocation(actualN1, n1);

        HoopoeProfiledInvocation actualN2 = actualRoot.getChildren().get(1);
        assertNotMergedInvocation(actualN2, n2);

        HoopoeProfiledInvocation actualN3 = actualRoot.getChildren().get(2);
        assertNotMergedInvocation(actualN3, n3);
    }

    @Test
    public void testNoSelfRecording() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        Thread selfThread = new Thread("HoopoeStorageImpl.thread");
        storage.consumeThreadTraceResults(selfThread, root);
        storage.waitForProvisioning();

        Collection<HoopoeProfiledInvocationSummary> actualSummaries = storage.getProfiledInvocationSummaries();
        assertThat(actualSummaries, notNullValue());
        assertThat(actualSummaries.size(), equalTo(0));
    }

    @Test
    public void testFastInvocationsAreTrimmed() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "cr", "mr");
        setTimeInMs(root, 0, 500);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c1", "m1");
        setTimeInMs(n1, 10, 300);

        HoopoeTraceNode n2 = new HoopoeTraceNode(n1, "c2", "m2");
        setTimeInNs(n2, msToNs(15), msToNs(15) + 999_999);  // duration less than 1ms

        HoopoeTraceNode n3 = new HoopoeTraceNode(root, "c3", "m3");
        setTimeInNs(n3, msToNs(320), msToNs(320) + 999_999);  // duration less than 1ms

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertInvocation(actualRoot, root);
        assertThat(actualRoot.getChildren().size(), equalTo(1));

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertInvocation(actualN1, n1);
        assertThat(actualN1.getChildren().size(), equalTo(0));
    }

    private void assertNotMergedInvocation(HoopoeProfiledInvocation actualCall, HoopoeTraceNode initialNode) {
        assertInvocation(actualCall, initialNode);
        assertThat(actualCall.getChildren().size(), equalTo(initialNode.getChildren().size()));
        assertThat(actualCall.getInvocationsCount(), equalTo(1));
    }

    private void assertInvocation(HoopoeProfiledInvocation actualCall, HoopoeTraceNode initialNode) {
        assertThat(actualCall, notNullValue());
        assertThat(actualCall.getClassName(), equalTo(initialNode.getClassName()));
        assertThat(actualCall.getMethodSignature(), equalTo(initialNode.getMethodSignature()));
        assertThat(actualCall.getChildren(), notNullValue());
    }

    private HoopoeProfiledInvocationSummary executeAndGetSummary(HoopoeTraceNode root) throws InterruptedException {
        storage.consumeThreadTraceResults(thread, root);
        storage.waitForProvisioning();
        return storage.getProfiledInvocationSummaries().iterator().next();
    }

    private HoopoeProfiledInvocation executeAndGetInvocation(HoopoeTraceNode root) throws InterruptedException {
        HoopoeProfiledInvocationSummary summary = executeAndGetSummary(root);
        return storage.getProfiledInvocation(summary.getId());
    }

    private void setTimeInMs(HoopoeTraceNode traceNode, long startTime, long endTime) throws Exception {
        setTimeInNs(traceNode, msToNs(startTime), msToNs(endTime));
    }

    private void setTimeInNs(HoopoeTraceNode traceNode, long startTime, long endTime) throws Exception {
        setFieldValue(traceNode, "startTime", startTime);
        setFieldValue(traceNode, "endTime", endTime);
    }

    private void setFieldValue(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    private long msToNs(long ms) {
        return ms * 1_000_000;
    }

}
