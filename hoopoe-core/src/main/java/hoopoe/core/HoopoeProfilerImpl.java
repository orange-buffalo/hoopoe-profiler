package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import hoopoe.api.HoopoeProfilerExtensionsProvider;
import hoopoe.core.bootstrap.HoopoeProfilerBridge;
import hoopoe.core.supplements.ConfigurationHelper;
import hoopoe.core.supplements.InstrumentationHelper;
import hoopoe.core.supplements.ProfiledResultHelper;
import hoopoe.core.supplements.TraceNode;
import hoopoe.core.supplements.TraceNodesWrapper;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    private List<PluginActionWrapper> pluginActions = new CopyOnWriteArrayList<>();

    private Collection<HoopoePlugin> plugins;

    private InstrumentationHelper instrumentationHelper;

    private ProfiledResultHelper profiledResultHelper = new ProfiledResultHelper();

    @Getter
    private HoopoeProfiledResult lastProfiledResult;

    public HoopoeProfilerImpl(String rawArgs, Instrumentation instrumentation) {
        log.info("starting profiler");

        ConfigurationHelper configurationHelper = new ConfigurationHelper();
        configuration = configurationHelper.getConfiguration(rawArgs);

        HoopoePluginsProvider pluginsProvider = configuration.createPluginsProvider();
        pluginsProvider.setupProfiler(this);
        plugins = pluginsProvider.createPlugins();

        HoopoeProfilerExtensionsProvider extensionsProvider = configuration.createProfilerExtensionProvider();
        extensionsProvider.setupProfiler(this);
        extensionsProvider.createExtensions().forEach(HoopoeProfilerExtension::init);

        instrumentationHelper =
                new InstrumentationHelper(configuration.getExcludedClassesPatterns(), this);
        instrumentationHelper.createClassFileTransformer(instrumentation);
    }

    public void unload() {
        instrumentationHelper.unload();
    }

    @Override
    public void startProfiling() {
        HoopoeProfilerBridge.enabled = true;
        lastProfiledResult = null;
    }

    @Override
    public HoopoeProfiledResult stopProfiling() {
        HoopoeProfilerBridge.enabled = false;

        lastProfiledResult = profiledResultHelper.calculateProfiledResult(profiledTraceNodeWrappers);
        profiledTraceNodeWrappers.clear();

        return lastProfiledResult;
    }

    @Override
    public boolean isProfiling() {
        return HoopoeProfilerBridge.enabled;
    }

    public void profileCall(long startTimeInNs,
                            long endTimeInNs,
                            String className,
                            String methodSignature,
                            int[] pluginActionIndicies,
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
        }
        else {
            Collection<HoopoeAttribute> attributes = new ArrayList<>(pluginActionIndicies.length);

            for (int pluginActionIndex : pluginActionIndicies) {
                PluginActionWrapper pluginActionWrapper = pluginActions.get(pluginActionIndex);
                attributes.addAll(pluginActionWrapper.pluginAction.getAttributes(
                        args, returnValue, thisInMethod,
                        traceNodesWrapper.getPluginCache(pluginActionWrapper.plugin)));
            }

            traceNodes.add(
                    new TraceNode(className, methodSignature, startTimeInNs, endTimeInNs, attributes));
        }
    }

    public List<Integer> addPluginActions(HoopoeMethodInfo methodInfo) {
        List<Integer> pluginActionIndicies = new ArrayList<>(0);
        for (HoopoePlugin plugin : plugins) {
            HoopoePluginAction pluginAction = plugin.createActionIfSupported(methodInfo);
            if (pluginAction != null) {
                pluginActions.add(new PluginActionWrapper(pluginAction, plugin));
                pluginActionIndicies.add(pluginActions.size() - 1);
            }
        }
        return pluginActionIndicies;
    }

    @AllArgsConstructor
    private static class PluginActionWrapper {
        HoopoePluginAction pluginAction;
        HoopoePlugin plugin;
    }

}