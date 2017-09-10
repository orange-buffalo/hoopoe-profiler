package hoopoe.core;

import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.api.plugins.HoopoeInvocationAttribute;
import hoopoe.core.components.ExtensionsManager;
import hoopoe.core.components.PluginsManager;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.instrumentation.CodeInstrumentation;
import hoopoe.core.supplements.ProfiledResultHelper;
import hoopoe.core.supplements.TraceNode;
import hoopoe.core.supplements.TraceNodesWrapper;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    // we know we will not be able to clean this thread local.
    // that's why we will clean the wrapper content, leaving tiny small wrapper hanging in thread local
    private static final ThreadLocal<TraceNodesWrapper> threadTraceNodesWrapper =
            ThreadLocal.withInitial(TraceNodesWrapper::new);

    private Collection<TraceNodesWrapper> profiledTraceNodeWrappers =
            Collections.synchronizedCollection(new ArrayList<>());

    @Getter
    private HoopoeConfiguration configuration;

    private CodeInstrumentation codeInstrumentation;

    private ProfiledResultHelper profiledResultHelper;

    @Getter
    private HoopoeProfiledResult lastProfiledResult;

    private PluginsManager pluginsManager;

    @Builder
    private HoopoeProfilerImpl(
            Configuration configuration,
            PluginsManager pluginsManager,
            CodeInstrumentation codeInstrumentation,
            ProfiledResultHelper profiledResultHelper,
            ExtensionsManager extensionsManager) {

        log.info("starting profiler");

        this.configuration = configuration;
        this.profiledResultHelper = profiledResultHelper;
        this.pluginsManager = pluginsManager;
        this.codeInstrumentation = codeInstrumentation;

        extensionsManager.initExtensions(this);

        HoopoeProfilerFacade.methodInvocationProfiler = this::profileMethodInvocation;
    }

    public void instrument(Instrumentation instrumentation) {
        codeInstrumentation.createClassFileTransformer(instrumentation);
    }

    public void unload() {
        codeInstrumentation.unload();
    }

    @Override
    public void startProfiling() {
        HoopoeProfilerFacade.startProfiling();
        lastProfiledResult = null;
    }

    @Override
    public HoopoeProfiledResult stopProfiling() {
        HoopoeProfilerFacade.enabled = false;

        lastProfiledResult = profiledResultHelper.calculateProfiledResult(profiledTraceNodeWrappers);
        profiledTraceNodeWrappers.clear();

        return lastProfiledResult;
    }

    @Override
    public boolean isProfiling() {
        return HoopoeProfilerFacade.enabled;
    }

    private void profileMethodInvocation(
            long startTimeInNs,
            long endTimeInNs,
            String className,
            String methodSignature,
            Object pluginActionIndicies,
            Object[] args,
            Object returnValue,
            Object thisInMethod) {

        TraceNodesWrapper traceNodesWrapper = threadTraceNodesWrapper.get();
        List<TraceNode> traceNodes = traceNodesWrapper.getTraceNodes();
        if (traceNodes == null) {
            traceNodes = traceNodesWrapper.init();
            threadTraceNodesWrapper.set(traceNodesWrapper);
            profiledTraceNodeWrappers.add(traceNodesWrapper);
        }

        if (pluginActionIndicies == null) {
            traceNodes.add(
                    new TraceNode(className, methodSignature, startTimeInNs, endTimeInNs, null));
        } else {
            Collection<HoopoeInvocationAttribute> attributes = pluginsManager.getRecorders(pluginActionIndicies).stream()
                    .flatMap(pluginAction -> pluginAction.getAttributes(args, returnValue, thisInMethod).stream())
                    .collect(Collectors.toList());

            traceNodes.add(new TraceNode(className, methodSignature, startTimeInNs, endTimeInNs, attributes));
        }
    }

}