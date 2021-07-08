package hoopoe.core.tracer;

import hoopoe.api.HoopoeProfiledInvocationRoot;
import hoopoe.api.HoopoeProfiledResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class TraceNormalizer {

    public HoopoeProfiledResult calculateProfiledResult(Collection<ThreadTracer> profiledTraceNodeWrappers) {
        Collection<Pair<String, TraceNode>> traceRoots = calculateTraceRoots(profiledTraceNodeWrappers);

        List<HoopoeProfiledInvocationRoot> invocationRoots = traceRoots.stream()
                .map(traceRoot -> new HoopoeProfiledInvocationRoot(
                        traceRoot.getLeft(), traceRoot.getRight().convertToProfiledInvocation()))
                .collect(Collectors.toList());
        return new HoopoeProfiledResult(invocationRoots);
    }

    private Collection<Pair<String, TraceNode>> calculateTraceRoots(
            Collection<ThreadTracer> profiledTraceNodeWrappers) {

        Collection<Pair<String, TraceNode>> traceRoots = new ArrayList<>();
        for (ThreadTracer traceNodesWrapper : profiledTraceNodeWrappers) {

            // nodes are naturally organized in a way, that child node is always before its parent node
            List<TraceNode> traceNodes = traceNodesWrapper.getTraceNodes();
            for (int currentNodeIndex = 0; currentNodeIndex < traceNodes.size(); currentNodeIndex++) {
                TraceNode currentNode = traceNodes.get(currentNodeIndex);
                boolean isRoot = true;
                for (int parentCandidateIndex = currentNodeIndex + 1; parentCandidateIndex < traceNodes.size(); parentCandidateIndex++) {
                    TraceNode parentCandidate = traceNodes.get(parentCandidateIndex);
                    if (parentCandidate.getStartTimeInNs() <= currentNode.getStartTimeInNs()
                            && parentCandidate.getEndTimeInNs() >= currentNode.getEndTimeInNs()) {
                        parentCandidate.addChild(currentNode);
                        isRoot = false;
                        break;
                    }
                }

                if (isRoot) {
                    traceRoots.add(Pair.of(traceNodesWrapper.getThreadName(), currentNode));
                }
            }

            traceNodesWrapper.clear();
        }
        return traceRoots;
    }

}
