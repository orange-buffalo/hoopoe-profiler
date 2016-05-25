package hoopoe.core;

import hoopoe.api.HoopoeHasAttributes;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeTracer;
import hoopoe.core.supplements.TraceNode;

public class HoopoeTracerImpl implements HoopoeTracer {

    private static final ThreadLocal<TraceNode> currentTraceNodeHolder = new ThreadLocal<>();

    private HoopoeProfiler profiler;

    @Override
    public HoopoeHasAttributes onMethodEnter(String className, String methodSignature) {
        TraceNode previousTraceNode = currentTraceNodeHolder.get();
        TraceNode currentTraceNode = new TraceNode(
                previousTraceNode,
                className,
                methodSignature,
                profiler.getConfiguration().getMinimumTrackedInvocationTimeInNs());
        currentTraceNodeHolder.set(currentTraceNode);
        return currentTraceNode;
    }

    @Override
    public HoopoeProfiledInvocation onMethodLeave() {
        TraceNode currentTraceNode = currentTraceNodeHolder.get();
        currentTraceNode.onMethodLeave();

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
