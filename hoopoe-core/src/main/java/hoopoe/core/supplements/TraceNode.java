package hoopoe.core.supplements;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeProfiledInvocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

public class TraceNode {

    @Getter
    private TraceNode parent;

    private List<TraceNode> children;

    private long methodEnterTimeInNs;

    private String className;

    private String methodSignature;

    private String groupingKey;

    @Setter
    private Collection<HoopoeAttribute> attributes;

    /**
     * Duration of this method body without descendant method calls and without profiler overhead.
     */
    private long ownTimeInNs;

    /**
     * Overhead by profiler. Includes own overhead + all immediate children overhead.
     */
    private long profilerOverheadInNs;

    /**
     * ownTimeInNs + totalTimeInNs of all children - profiler overhead of all immediate children
     */
    private long totalTimeInNs;

    private int invocationsCount;

    private long trimThresholdInNs;

    public TraceNode(TraceNode parent, String className, String methodSignature, long trimThresholdInNs) {
        this.parent = parent;
        this.className = className;
        this.methodSignature = methodSignature;
        this.trimThresholdInNs = trimThresholdInNs;
        this.methodEnterTimeInNs = System.nanoTime();
        this.invocationsCount = 1;
        if (parent != null) {
            if (parent.children == null) {
                parent.children = new ArrayList<>(1);
            }
            parent.children.add(this);
        }
    }

    public void onMethodLeave() {
        long methodLeaveTimeInNs = System.nanoTime();

        // this gives overhead, we try to compensate it calculating profilerOverheadInNs
        long childrenTotalTimeInNs = 0;
        long childrenProfilerOverheadInNs = 0;
        if (children != null) {
            children = children.stream()
                    .collect(getMergeTraceNodesCollector())
                    .values().stream()
                    .filter(traceNode -> traceNode.totalTimeInNs >= trimThresholdInNs)
                    .collect(Collectors.toList());

            for (TraceNode childNode : children) {
                childrenTotalTimeInNs += childNode.totalTimeInNs;
                childrenProfilerOverheadInNs += childNode.profilerOverheadInNs;
            }
        }

        // children profile overhead is a part of descendant calls execution time and should be deducted
        totalTimeInNs = methodLeaveTimeInNs - methodEnterTimeInNs - childrenProfilerOverheadInNs;
        ownTimeInNs = totalTimeInNs - childrenTotalTimeInNs;
        profilerOverheadInNs = System.nanoTime() - methodLeaveTimeInNs + childrenProfilerOverheadInNs;
    }

    private static Collector<TraceNode, ?, Map<String, TraceNode>> getMergeTraceNodesCollector() {
        return Collectors.groupingBy(
                TraceNode::getGroupingKey,
                Collectors.reducing(
                        null,
                        Function.identity(),
                        TraceNode::mergeNodes
                ));
    }

    public HoopoeProfiledInvocation convertToProfiledInvocation() {
        return convertToProfiledInvocation(this);
    }

    private static TraceNode mergeNodes(TraceNode aggregated, TraceNode nextNode) {
        if (aggregated == null) {
            return nextNode;
        }
        aggregated.totalTimeInNs += nextNode.totalTimeInNs;
        aggregated.ownTimeInNs += nextNode.ownTimeInNs;
        aggregated.profilerOverheadInNs += nextNode.profilerOverheadInNs;
        aggregated.invocationsCount += nextNode.invocationsCount;
        if (nextNode.children != null) {
            if (aggregated.children == null) {
                aggregated.children = nextNode.children;
            }
            else {
                aggregated.children.addAll(nextNode.children);
            }
        }
        // class name, method signature and attributes are part of grouping criteria,
        // thus they are the same and no need to merge
        return aggregated;
    }

    private static HoopoeProfiledInvocation convertToProfiledInvocation(TraceNode traceNode) {
        List<HoopoeProfiledInvocation> invocationChildren = new ArrayList<>(0);
        if (traceNode.children != null) {
            traceNode.children = traceNode.children.stream()
                    .collect(getMergeTraceNodesCollector())
                    .values().stream()
                    .sorted((o1, o2) -> Long.compare(o2.totalTimeInNs, o1.totalTimeInNs))
                    .collect(Collectors.toList());

            traceNode.children.forEach(
                    childNode -> invocationChildren.add(convertToProfiledInvocation(childNode)));
        }

        return new HoopoeProfiledInvocation(
                traceNode.className,
                traceNode.methodSignature,
                invocationChildren,
                traceNode.totalTimeInNs,
                traceNode.ownTimeInNs,
                traceNode.invocationsCount,
                traceNode.attributes == null ? Collections.emptyList() : traceNode.attributes
        );
    }

    private String getGroupingKey() {
        if (groupingKey == null) {
            String attributesKey = StringUtils.EMPTY;
            if (attributes != null) {
                List<HoopoeAttribute> sortedAttributes = new ArrayList(attributes);
                Collections.sort(sortedAttributes, (o1, o2) -> o1.getName().compareTo(o2.getName()));
                for (HoopoeAttribute attribute : sortedAttributes) {
                    attributesKey += attribute.getName() + attribute.getDetails();
                }
            }
            groupingKey = className + methodSignature + attributesKey;
        }
        return groupingKey;
    }

}