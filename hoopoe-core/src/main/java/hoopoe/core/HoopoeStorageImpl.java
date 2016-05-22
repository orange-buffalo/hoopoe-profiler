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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class HoopoeStorageImpl implements HoopoeProfilerStorage {

    private static final String STORAGE_THREAD_NAME = "HoopoeStorageImpl.thread";

    private final Map<String, HoopoeProfiledInvocation> invocations =
            Collections.synchronizedMap(new HashMap<>());

    private final Collection<HoopoeProfiledInvocationSummary> invocationSummaries =
            Collections.synchronizedCollection(new ArrayList<>());

    private AtomicInteger invocationsIdGenerator = new AtomicInteger(0);

    private ExecutorService executorService = Executors.newCachedThreadPool(
            target -> new Thread(target, STORAGE_THREAD_NAME));

    @Override
    public void consumeThreadTraceResults(Thread thread, HoopoeTraceNode traceRoot) {
        if (STORAGE_THREAD_NAME.equals(thread.getName())) {
            return;
        }
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

    /**
     * A hook for tests only. Shutdowns the internal executor and waits for all submitted tasks to complete.
     * It is illegal to call consumeThreadTraceResults after.
     */
    public void waitForProvisioning() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
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
        Collections.sort(attributeSummaries, this::compareAttributes);
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
        List<HoopoeProfiledInvocation> children = new ArrayList<>(nodeChildren.stream()
                .map(this::processTraceNode)
                .collect(Collectors.groupingBy(
                        this::getMergeGroupingKey,
                        Collectors.reducing(
                                null,
                                Function.identity(),
                                this::mergeInvocations
                        )
                ))
                .values());
        Collections.sort(children, this::compareInvocationsByTotalTime);

        long subInvocationsTimeInNs = children.stream()
                .mapToLong(HoopoeProfiledInvocation::getTotalTimeInNs)
                .sum();
        long totalTimeInNs = node.getDurationInNs();

        return new HoopoeProfiledInvocation(
                node.getClassName(),
                node.getMethodSignature(),
                children,
                totalTimeInNs,
                totalTimeInNs - subInvocationsTimeInNs,
                1,
                node.getAttributes()
        );
    }

    /**
     * Sorts calls by duration, slower call goes first
     */
    private int compareInvocationsByTotalTime(HoopoeProfiledInvocation o1, HoopoeProfiledInvocation o2) {
        return Long.compare(o2.getTotalTimeInNs(), o1.getTotalTimeInNs());
    }

    /**
     * Merge invocations with the same class, method and attributes
     */
    private String getMergeGroupingKey(HoopoeProfiledInvocation invocation) {
        String attributesKey = "";
        List<HoopoeAttribute> attributes = new ArrayList<>(invocation.getAttributes());
        Collections.sort(attributes, this::compareAttributes);
        for (HoopoeAttribute attribute : attributes) {
            attributesKey += attribute.getName() + attribute.getDetails();
        }
        return invocation.getClassName() + invocation.getMethodSignature() + attributesKey;
    }

    private HoopoeProfiledInvocation mergeInvocations(HoopoeProfiledInvocation aggregated,
                                                      HoopoeProfiledInvocation next) {
        if (aggregated == null) {
            return next;
        }

        List<HoopoeProfiledInvocation> children = new ArrayList<>(
                aggregated.getChildren().size() + next.getChildren().size());
        children.addAll(aggregated.getChildren());
        children.addAll(next.getChildren());
        Collections.sort(children, this::compareInvocationsByTotalTime);

        return new HoopoeProfiledInvocation(
                aggregated.getClassName(),
                aggregated.getMethodSignature(),
                children,
                aggregated.getTotalTimeInNs() + next.getTotalTimeInNs(),
                aggregated.getOwnTimeInNs() + next.getOwnTimeInNs(),
                aggregated.getInvocationsCount() + next.getInvocationsCount(),
                aggregated.getAttributes()
        );
    }

    private int compareAttributes(HoopoeAttribute o1, HoopoeAttribute o2) {
        int result = o1.getName().compareTo(o2.getName());
        if (result == 0 && o1.getDetails() != null && o2.getDetails() != null) {
            result = o1.getDetails().compareTo(o2.getDetails());
        }
        return result;
    }

}
