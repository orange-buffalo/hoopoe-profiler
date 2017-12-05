package hoopoe.core.tracer;

import hoopoe.api.HoopoeInvocationAttribute;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledInvocationRoot;
import hoopoe.api.HoopoeProfiledResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class HotSpotCalculator {

    public HoopoeProfiledResult calculateHotSpots(HoopoeProfiledResult profiledResult, int hotSpotsCountPerRoot) {
        return new HoopoeProfiledResult(profiledResult.getInvocations().stream()
                .flatMap(root -> calculateRootHotSpots(root, hotSpotsCountPerRoot).stream())
                .collect(Collectors.toList()));
    }

    private List<HoopoeProfiledInvocationRoot> calculateRootHotSpots(
            HoopoeProfiledInvocationRoot root,
            int hotSpotsCount) {

        Map<String, HotSpot> hotSpots = new HashMap<>();
        processForHotSpot(root.getInvocation(), null, hotSpots);

        return hotSpots.values().stream()
                .sorted(Comparator.comparing(HotSpot::getOwnTimeInNs)
                        .thenComparing(HotSpot::getTotalTimeInNs)
                        .thenComparing(hotSpot -> invocationToKey(hotSpot.getInvocations().iterator().next().source))
                        .reversed()
                )
                .limit(hotSpotsCount)
                .map(hotSpot -> toInvocationRoot(hotSpot, root.getThreadName()))
                .collect(Collectors.toList());
    }

    private HoopoeProfiledInvocationRoot toInvocationRoot(HotSpot hotSpot, String threadName) {
        return new HoopoeProfiledInvocationRoot(threadName, merge(hotSpot.invocations));
    }

    private HoopoeProfiledInvocation merge(Collection<BidirectionalInvocation> bidirectionalInvocations) {
        List<HoopoeProfiledInvocation> invocations = bidirectionalInvocations.stream()
                .map(BidirectionalInvocation::getSource)
                .collect(Collectors.toList());

        List<HoopoeInvocationAttribute> attributes = new ArrayList<>();
        long totalTimeInNs = 0;
        long ownTimeInNs = 0;
        int invocationsCount = 0;

        for (HoopoeProfiledInvocation invocation : invocations) {
            totalTimeInNs += invocation.getTotalTimeInNs();
            ownTimeInNs += invocation.getOwnTimeInNs();
            invocationsCount += invocation.getInvocationsCount();
            attributes.addAll(invocation.getAttributes());
        }

        List<HoopoeProfiledInvocation> children = bidirectionalInvocations.stream()
                .filter(bidirectionalInvocation -> bidirectionalInvocation.parent != null)
                .map(BidirectionalInvocation::getParent)
                .collect(Collectors.groupingBy(
                        bidirectionalInvocation -> invocationToKey(bidirectionalInvocation.source),
                        Collectors.toList()
                ))
                .values().stream()
                .map(this::merge)
                .sorted(Comparator.comparing(HoopoeProfiledInvocation::getOwnTimeInNs)
                        .thenComparing(HoopoeProfiledInvocation::getTotalTimeInNs)
                        .thenComparing(HoopoeProfiledInvocation::getClassName)
                        .thenComparing(HoopoeProfiledInvocation::getMethodSignature)
                        .reversed()
                )
                .collect(Collectors.toList());

        HoopoeProfiledInvocation invocation = invocations.iterator().next();
        return new HoopoeProfiledInvocation(
                invocation.getClassName(),
                invocation.getMethodSignature(),
                children,
                totalTimeInNs,
                ownTimeInNs,
                invocationsCount,
                attributes
        );
    }

    private void processForHotSpot(
            HoopoeProfiledInvocation invocation,
            BidirectionalInvocation parent,
            Map<String, HotSpot> hotSpots) {

        BidirectionalInvocation bidirectionalInvocation = new BidirectionalInvocation(invocation, parent);

        HotSpot hotSpot = hotSpots.computeIfAbsent(invocationToKey(invocation), key -> new HotSpot());
        hotSpot.registerInvocation(bidirectionalInvocation);

        invocation.getChildren().forEach(child -> processForHotSpot(child, bidirectionalInvocation, hotSpots));
    }

    private String invocationToKey(HoopoeProfiledInvocation invocation) {
        return invocation.getClassName() + invocation.getMethodSignature();
    }

    @AllArgsConstructor
    @Getter
    private static class BidirectionalInvocation {
        HoopoeProfiledInvocation source;
        BidirectionalInvocation parent;
    }

    @Getter
    private static class HotSpot {
        long totalTimeInNs;
        long ownTimeInNs;
        Collection<BidirectionalInvocation> invocations = new ArrayList<>();

        public void registerInvocation(BidirectionalInvocation bidirectionalInvocation) {
            this.invocations.add(bidirectionalInvocation);
            this.ownTimeInNs += bidirectionalInvocation.source.getOwnTimeInNs();
            this.totalTimeInNs += bidirectionalInvocation.source.getTotalTimeInNs();
        }
    }


}
