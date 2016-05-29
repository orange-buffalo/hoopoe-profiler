package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeTracer;
import hoopoe.core.supplements.TraceNode;
import java.util.Collection;

public class HoopoeTracerImpl implements HoopoeTracer {

    private static final ThreadLocal<TraceNode> currentTraceNodeHolder = new ThreadLocal<>();

    private HoopoeProfiler profiler;

    @Override
    public void onMethodEnter(String className, String methodSignature) {
        TraceNode previousTraceNode = currentTraceNodeHolder.get();
        TraceNode currentTraceNode = new TraceNode(
                previousTraceNode,
                className,
                methodSignature,
                profiler.getConfiguration().getMinimumTrackedInvocationTimeInNs());
        currentTraceNodeHolder.set(currentTraceNode);
    }

    @Override
    public HoopoeProfiledInvocation onMethodLeave(Collection<HoopoeAttribute> attributes, long profilerOverheadInNs) {
        TraceNode currentTraceNode = currentTraceNodeHolder.get();
        currentTraceNode.setAttributes(attributes);
        currentTraceNode.onMethodLeave(profilerOverheadInNs);

        TraceNode previousTraceNode = currentTraceNode.getParent();
        currentTraceNodeHolder.set(previousTraceNode);
        if (previousTraceNode == null) {
            return currentTraceNode.convertToProfiledInvocation();
        }

        return null;
    }

    @Override
    public void setupProfiler(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }
}
