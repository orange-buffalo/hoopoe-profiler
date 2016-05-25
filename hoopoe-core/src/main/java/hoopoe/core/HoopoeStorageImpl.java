package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeAttributeSummary;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationSummary;
import hoopoe.api.HoopoeProfilerStorage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

public class HoopoeStorageImpl implements HoopoeProfilerStorage {

    private final Map<String, HoopoeProfiledInvocation> invocations =
            Collections.synchronizedMap(new HashMap<>());

    private final Collection<HoopoeProfiledInvocationSummary> invocationSummaries =
            Collections.synchronizedCollection(new ArrayList<>());

    private AtomicInteger invocationsIdGenerator = new AtomicInteger(0);

    @Override
    public void addInvocation(Thread thread, HoopoeProfiledInvocation invocation) {
        LocalDateTime profiledOn = LocalDateTime.now();
        String invocationId = String.valueOf(invocationsIdGenerator.incrementAndGet());

        List<HoopoeAttributeSummary> attributeSummaries = getAttributeSummaries(invocation);

        HoopoeProfiledInvocationSummary summary = new HoopoeProfiledInvocationSummary(
                thread.getName(), profiledOn, invocationId, invocation.getTotalTimeInNs(), attributeSummaries);
        invocationSummaries.add(summary);
        invocations.putIfAbsent(invocationId, invocation);
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


    private List<HoopoeAttributeSummary> getAttributeSummaries(HoopoeProfiledInvocation invocation) {
        List<HoopoeAttributeSummary> attributeSummaries = new ArrayList<>(0);
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

    private int compareAttributes(HoopoeAttribute o1, HoopoeAttribute o2) {
        int result = o1.getName().compareTo(o2.getName());
        if (result == 0 && o1.getDetails() != null && o2.getDetails() != null) {
            result = o1.getDetails().compareTo(o2.getDetails());
        }
        return result;
    }

}
