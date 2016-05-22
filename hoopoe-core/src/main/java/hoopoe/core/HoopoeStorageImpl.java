package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeAttributeSummary;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationSummary;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeTraceNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class HoopoeStorageImpl implements HoopoeProfilerStorage {

    private final Map<String, HoopoeProfiledInvocation> invocations =
            Collections.synchronizedMap(new HashMap<>());
    private final Collection<HoopoeProfiledInvocationSummary> invocationSummaries =
            Collections.synchronizedCollection(new ArrayList<>());
    private AtomicInteger invocationsIdGenerator = new AtomicInteger(0);
    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void consumeThreadTraceResults(Thread thread, HoopoeTraceNode traceRoot) {
        LocalDateTime profiledOn = LocalDateTime.now();
        executorService.execute(() -> {
            processTraceResults(thread.getName(), profiledOn, traceRoot);
        });
    }

    @Override
    public Collection<HoopoeProfiledInvocationSummary> getProfiledInvocationSummaries() {
        synchronized (invocationSummaries) {
            return new ArrayList<>(invocationSummaries);
        }
    }

    @Override
    public HoopoeProfiledInvocation getProfiledInvocation(String id) {
        return invocations.get(id);
    }

    private void processTraceResults(String threadName, LocalDateTime profiledOn, HoopoeTraceNode traceRoot) {
        HoopoeProfiledInvocation invocation = processTraceNode(traceRoot);
        String invocationId = String.valueOf(invocationsIdGenerator.incrementAndGet());

        List<HoopoeAttributeSummary> attributeSummaries = getAttributeSummaries(invocation);

        HoopoeProfiledInvocationSummary summary = new HoopoeProfiledInvocationSummary(
                threadName, profiledOn, invocationId, invocation.getTotalTimeInNs(), attributeSummaries);
        invocationSummaries.add(summary);
        invocations.putIfAbsent(invocationId, invocation);
    }

    private List<HoopoeAttributeSummary> getAttributeSummaries(HoopoeProfiledInvocation invocation) {
        List<HoopoeAttributeSummary> attributeSummaries = new ArrayList<>();
        collectAttributeSummaries(invocation, attributeSummaries);
        Collections.sort(attributeSummaries, (o1, o2) -> {
            int result = o1.getName().compareTo(o2.getName());
            if (result == 0 && o1.getDetails() != null && o2.getDetails() != null) {
                result = o1.getDetails().compareTo(o2.getDetails());
            }
            return result;
        });
        return attributeSummaries;
    }

    private void collectAttributeSummaries(HoopoeProfiledInvocation invocation,
                                           Collection<HoopoeAttributeSummary> attributeSummaries) {
        for (HoopoeAttribute attribute : invocation.getAttributes()) {
            HoopoeAttributeSummary summary = null;
            for (Iterator<HoopoeAttributeSummary> summaryIterator = attributeSummaries.iterator(); summaryIterator.hasNext(); ) {
                HoopoeAttributeSummary mergeCandidate = summaryIterator.next();
                if (StringUtils.equals(attribute.getName(), mergeCandidate.getName())
                        && StringUtils.equals(attribute.getDetails(), mergeCandidate.getDetails())) {
                    summary = new HoopoeAttributeSummary(
                            mergeCandidate.getName(),
                            mergeCandidate.getDetails(),
                            mergeCandidate.isContributingTime(),
                            mergeCandidate.getTotalTimeInNs() + invocation.getTotalTimeInNs(),
                            mergeCandidate.getTotalOccurrences() + 1);
                    summaryIterator.remove();
                    break;
                }
            }

            if (summary == null) {
                summary = new HoopoeAttributeSummary(
                        attribute.getName(),
                        attribute.getDetails(),
                        attribute.isContributingTime(),
                        invocation.getTotalTimeInNs(),
                        1
                );
            }

            attributeSummaries.add(summary);
        }

        for (HoopoeProfiledInvocation child : invocation.getChildren()) {
            collectAttributeSummaries(child, attributeSummaries);
        }
    }

    private HoopoeProfiledInvocation processTraceNode(HoopoeTraceNode node) {
        List<HoopoeTraceNode> nodeChildren = node.getChildren();
        List<HoopoeProfiledInvocation> subInvocations = nodeChildren.stream()
                .map(this::processTraceNode)
                .sorted(this::compareInvocationsByTotalTime)
                .collect(Collectors.toList());

        long subInvocationsTimeInNs = subInvocations.stream()
                .mapToLong(HoopoeProfiledInvocation::getTotalTimeInNs)
                .sum();
        long totalTimeInNs = node.getDurationInNs();

        HoopoeProfiledInvocation invocation = new HoopoeProfiledInvocation(
                node.getClassName(),
                node.getMethodSignature(),
                subInvocations,
                totalTimeInNs,
                totalTimeInNs - subInvocationsTimeInNs,
                node.getAttributes()
        );

        return invocation;
    }

    /**
     * A hook for tests only. Shutdowns the internal executor and waits for all submitted tasks to complete.
     * It is illegal to call consumeThreadTraceResults after.
     */
    public void waitForProvisioning() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }

    /**
     * Sorts calls by duration, slower call goes first
     */
    private int compareInvocationsByTotalTime(HoopoeProfiledInvocation o1, HoopoeProfiledInvocation o2) {
        return Long.compare(o2.getTotalTimeInNs(), o1.getTotalTimeInNs());
    }
}
