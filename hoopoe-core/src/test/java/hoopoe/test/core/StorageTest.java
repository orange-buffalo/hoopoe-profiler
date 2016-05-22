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
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
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
        assertNotMergedProfiledCall(actualInvocation, root);
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
        assertNotMergedProfiledCall(actualInvocation, root1);

        actualSummary = actualSummariesIterator.next();
        assertThat(actualSummary.getThreadName(), equalTo(THREAD_NAME));
        assertThat(actualSummary.getProfiledOn(), notNullValue());
        assertThat(actualSummary.getId(), notNullValue());

        actualInvocation = storage.getProfiledInvocation(actualSummary.getId());
        assertNotMergedProfiledCall(actualInvocation, root2);
    }

    @Test
    public void testMethodCallHierarchy() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");

        HoopoeTraceNode n21 = new HoopoeTraceNode(n1, "c3", "m3");
        setTimeInMs(n21, 0, 100);

        // this child execution is slower, it should be the first in profiled results
        HoopoeTraceNode n22 = new HoopoeTraceNode(n1, "c4", "m4");
        setTimeInMs(n22, 150, 500);

        HoopoeProfiledInvocation actualRoot = executeAndGetInvocation(root);
        assertNotMergedProfiledCall(actualRoot, root);

        HoopoeProfiledInvocation actualN1 = actualRoot.getChildren().get(0);
        assertNotMergedProfiledCall(actualN1, n1);

        // remember, 2.2 is slower than 2.1 and should be in the list before
        HoopoeProfiledInvocation actualN22 = actualN1.getChildren().get(0);
        assertNotMergedProfiledCall(actualN22, n22);

        HoopoeProfiledInvocation actualN21 = actualN1.getChildren().get(1);
        assertNotMergedProfiledCall(actualN21, n21);
    }

    @Test
    public void testTimeCalculations() throws Exception {
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n1, 5, 80);

        HoopoeTraceNode n2 = new HoopoeTraceNode(n1, "c3", "m3");
        setTimeInMs(n2, 6, 30);

        HoopoeTraceNode n3 = new HoopoeTraceNode(n1, "c4", "m4");
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
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c3", "m3");
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
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query1", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c3", "m3");
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
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n1, 5, 80);
        n1.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c3", "m3");
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
        HoopoeTraceNode root = new HoopoeTraceNode(null, "c1", "m1");
        setTimeInMs(root, 0, 100);

        HoopoeTraceNode n1 = new HoopoeTraceNode(root, "c2", "m2");
        setTimeInMs(n1, 5, 80);
        root.addAttribute(new HoopoeAttribute("sql", "query", true));

        HoopoeTraceNode n2 = new HoopoeTraceNode(root, "c3", "m3");
        setTimeInMs(n2, 85, 90);
        root.addAttribute(new HoopoeAttribute("sql", "query", true));
    }

    private void assertNotMergedProfiledCall(HoopoeProfiledInvocation actualCall, HoopoeTraceNode initialNode) {
        assertThat(actualCall, notNullValue());
        assertThat(actualCall.getClassName(), equalTo(initialNode.getClassName()));
        assertThat(actualCall.getMethodSignature(), equalTo(initialNode.getMethodSignature()));
        assertThat(actualCall.getChildren(), notNullValue());
        assertThat(actualCall.getChildren().size(), equalTo(initialNode.getChildren().size()));
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
