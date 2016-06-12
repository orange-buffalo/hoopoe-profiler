package hoopoe.core.supplements;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeProfiledInvocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class TraceNode {

    private List<TraceNode> children;

    private String className;

    private String methodSignature;

    @Getter
    private long startTimeInNs;

    @Getter
    private long endTimeInNs;

    private Collection<HoopoeAttribute> attributes;

    /**
     * Duration of this method body without descendant method calls and without profiler overhead.
     */
    private long ownTimeInNs;

    /**
     * Accumulative profiler overhead of this method and all its children.
     */
    private long profilerOverheadInNs;

    /**
     * ownTimeInNs + totalTimeInNs of all children (profiler overhead is excluded).
     */
    private long totalTimeInNs;

    private int invocationsCount;

    public TraceNode(String className,
                     String methodSignature,
                     long startTimeInNs,
                     long endTimeInNs,
                     Collection<HoopoeAttribute> attributes) {
        this.className = className;
        this.methodSignature = methodSignature;
        this.startTimeInNs = startTimeInNs;
        this.endTimeInNs = endTimeInNs;
        this.attributes = attributes;
        this.invocationsCount = 1;
        this.profilerOverheadInNs = System.nanoTime() - endTimeInNs;
        this.totalTimeInNs = Math.max(0, endTimeInNs - startTimeInNs - profilerOverheadInNs);
        this.ownTimeInNs = this.totalTimeInNs;
    }

    public HoopoeProfiledInvocation convertToProfiledInvocation() {
        return convertToProfiledInvocation(this);
    }

    public void addChild(TraceNode child) {
        if (children == null) {
            children = new LinkedList<>();
        }
        children.add(child);
        profilerOverheadInNs += child.profilerOverheadInNs;
        ownTimeInNs = Math.max(0, ownTimeInNs - child.totalTimeInNs - child.profilerOverheadInNs);
        totalTimeInNs = Math.max(0, totalTimeInNs - child.profilerOverheadInNs);
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
        String attributesKey = StringUtils.EMPTY;
        if (attributes != null) {
            List<HoopoeAttribute> sortedAttributes = new ArrayList(attributes);
            Collections.sort(sortedAttributes, (o1, o2) -> o1.getName().compareTo(o2.getName()));
            for (HoopoeAttribute attribute : sortedAttributes) {
                attributesKey += attribute.getName() + attribute.getDetails();
            }
        }
        return className + methodSignature + attributesKey;
    }

}