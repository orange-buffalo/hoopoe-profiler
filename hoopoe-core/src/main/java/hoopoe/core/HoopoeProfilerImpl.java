package hoopoe.core;

import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.api.HoopoeInvocationAttribute;
import hoopoe.core.components.ExtensionsManager;
import hoopoe.core.components.PluginsManager;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.instrumentation.CodeInstrumentation;
import hoopoe.core.tracer.HotSpotCalculator;
import hoopoe.core.tracer.TraceNormalizer;
import hoopoe.core.tracer.TraceNode;
import hoopoe.core.tracer.ThreadTracer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    // we know we will not be able to clean this thread local.
    // that's why we will clean the wrapper content, leaving tiny small wrapper hanging in thread local
    private static final ThreadLocal<ThreadTracer> threadTracer = ThreadLocal.withInitial(ThreadTracer::new);

    private Collection<ThreadTracer> threadTracers = Collections.synchronizedCollection(new ArrayList<>());

    @Getter
    private HoopoeConfiguration configuration;

    private CodeInstrumentation codeInstrumentation;

    private TraceNormalizer traceNormalizer;

    @Getter
    private HoopoeProfiledResult lastProfiledResult;

    private PluginsManager pluginsManager;

    private HotSpotCalculator hotSpotCalculator;

    @Builder
    private HoopoeProfilerImpl(
            Configuration configuration,
            PluginsManager pluginsManager,
            CodeInstrumentation codeInstrumentation,
            TraceNormalizer traceNormalizer,
            ExtensionsManager extensionsManager,
            HotSpotCalculator hotSpotCalculator) {

        log.info("starting profiler");

        this.hotSpotCalculator = hotSpotCalculator;
        this.configuration = configuration;
        this.traceNormalizer = traceNormalizer;
        this.pluginsManager = pluginsManager;
        this.codeInstrumentation = codeInstrumentation;

        extensionsManager.initExtensions(this);

        HoopoeProfilerFacade.methodInvocationProfiler = this::profileMethodInvocation;
    }

    public void instrument(Instrumentation instrumentation) {
        codeInstrumentation.createClassFileTransformer(instrumentation);
    }

    @Override
    public void startProfiling() {
        HoopoeProfilerFacade.startProfiling();
        lastProfiledResult = null;
    }

    @Override
    public HoopoeProfiledResult stopProfiling() {
        HoopoeProfilerFacade.enabled = false;

        lastProfiledResult = traceNormalizer.calculateProfiledResult(threadTracers);
        threadTracers.clear();

        return lastProfiledResult;
    }

    @Override
    public boolean isProfiling() {
        return HoopoeProfilerFacade.enabled;
    }

    @Override
    public HoopoeProfiledResult calculateHotSpots(int hotSpotsCountPerRoot) {
        return hotSpotCalculator.calculateHotSpots(this.lastProfiledResult, hotSpotsCountPerRoot);
    }

    private void profileMethodInvocation(
            long startTimeInNs,
            long endTimeInNs,
            String className,
            String methodSignature,
            long pluginRecordersReference,
            Object[] args,
            Object returnValue,
            Object thisInMethod) {

        ThreadTracer threadTracer = HoopoeProfilerImpl.threadTracer.get();
        List<TraceNode> traceNodes = threadTracer.getTraceNodes();
        if (traceNodes == null) {
            traceNodes = threadTracer.init();
            HoopoeProfilerImpl.threadTracer.set(threadTracer);
            threadTracers.add(threadTracer);
        }

        if (pluginRecordersReference == 0) {
            traceNodes.add(new TraceNode(className, methodSignature, startTimeInNs, endTimeInNs, null));

        } else {
            Collection<HoopoeInvocationAttribute> attributes = pluginsManager.getRecorders(pluginRecordersReference)
                    .stream()
                    .flatMap(pluginRecorder -> pluginRecorder.getAttributes(args, returnValue, thisInMethod).stream())
                    .collect(Collectors.toList());

            traceNodes.add(new TraceNode(className, methodSignature, startTimeInNs, endTimeInNs, attributes));
        }
    }

}