package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeThreadLocalCache;
import hoopoe.core.supplements.ConfigurationHelper;
import hoopoe.core.supplements.HoopoeThreadLocalCacheImpl;
import hoopoe.core.supplements.InstrumentationHelper;
import hoopoe.core.supplements.TraceNode;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    private static final ThreadLocal<ThreadProfileData> threadProfileDataHolder = new ThreadLocal<>();

    /**
     * A hook for tests. Otherwise it is extremely hard to disable profiler after test execution.
     */
    private ClassFileTransformer classFileTransformer;  // todo implement unload in this class?

    private HoopoeProfilerStorage storage;

    @Getter
    private HoopoeConfiguration configuration;

    private List<PluginActionWrapper> pluginActions = new CopyOnWriteArrayList<>();

    private Collection<HoopoePlugin> plugins;

    private Map<HoopoePlugin, HoopoeThreadLocalCache> pluginsThreadLocalCaches = new HashMap<>();

    public HoopoeProfilerImpl(String rawArgs, Instrumentation instrumentation) {
        log.info("starting profiler");

        ConfigurationHelper configurationHelper = new ConfigurationHelper();
        configuration = configurationHelper.getConfiguration(rawArgs);
        storage = configuration.createProfilerStorage();

        HoopoePluginsProvider pluginsProvider = configuration.createPluginsProvider();
        pluginsProvider.setupProfiler(this);
        plugins = pluginsProvider.createPlugins();

        InstrumentationHelper instrumentationHelper =
                new InstrumentationHelper(configuration.getExcludedClassesPatterns(), this);
        classFileTransformer = instrumentationHelper.createClassFileTransformer(instrumentation);
    }

    public void profileCall(long startTimeInNs,
                            long endTimeInNs,
                            String className,
                            String methodSignature,
                            int[] pluginActionIndicies,
                            Object[] args,
                            Object returnValue,
                            Object thisInMethod) {

        ThreadProfileData threadProfileData = threadProfileDataHolder.get();
        if (threadProfileData == null) {
            threadProfileData = new ThreadProfileData(startTimeInNs);
            threadProfileDataHolder.set(threadProfileData);
        }

        if (threadProfileData.terminated) {
            return;
        }

        if (pluginActionIndicies == null) {
            threadProfileData.traceNodes.add(new TraceNode(
                    className, methodSignature, startTimeInNs, endTimeInNs, null));
        }
        else {
            Collection<HoopoeAttribute> attributes = new ArrayList<>(pluginActionIndicies.length);

            for (int pluginActionIndex : pluginActionIndicies) {
                PluginActionWrapper pluginActionWrapper = pluginActions.get(pluginActionIndex);
                attributes.addAll(
                        pluginActionWrapper.pluginAction.getAttributes(
                                args, returnValue, thisInMethod, pluginActionWrapper.cache));
            }

            threadProfileData.traceNodes.add(new TraceNode(
                    className, methodSignature, startTimeInNs, endTimeInNs, attributes));
        }
    }

    public void finishThreadProfiling() {
        ThreadProfileData threadProfileData = threadProfileDataHolder.get();
        if (threadProfileData == null) {
            return;
        }

        // nodes are naturally organized in a way, that child node is always before its parent node
        List<TraceNode> traceNodes = threadProfileData.traceNodes;
        for (int currentNodeIndex = 0; currentNodeIndex < traceNodes.size(); currentNodeIndex++) {
            TraceNode currentNode = traceNodes.get(currentNodeIndex);
            for (int parentCandidateIndex = currentNodeIndex + 1; parentCandidateIndex < traceNodes.size(); parentCandidateIndex++) {
                TraceNode parentCandidate = traceNodes.get(parentCandidateIndex);
                if (parentCandidate.getStartTimeInNs() <= currentNode.getStartTimeInNs() &&
                        parentCandidate.getEndTimeInNs() >= currentNode.getEndTimeInNs()) {
                    parentCandidate.addChild(currentNode);
                    break;
                }
            }
        }

        TraceNode root = traceNodes.get(traceNodes.size() - 1);
        HoopoeProfiledInvocation profiledInvocation = root.convertToProfiledInvocation();
        storage.addInvocation(Thread.currentThread(), profiledInvocation);

        for (PluginActionWrapper pluginActionWrapper : pluginActions) {
            pluginActionWrapper.cache.clear();
        }

        threadProfileDataHolder.remove();
    }

    public List<Integer> addPluginActions(HoopoeMethodInfo methodInfo) {
        List<Integer> pluginActionIndicies = new ArrayList<>(0);
        for (HoopoePlugin plugin : plugins) {
            HoopoePluginAction pluginAction = plugin.createActionIfSupported(methodInfo);
            if (pluginAction != null) {
                HoopoeThreadLocalCache cache = pluginsThreadLocalCaches.get(plugin);
                if (cache == null) {
                    cache = new HoopoeThreadLocalCacheImpl();
                    pluginsThreadLocalCaches.put(plugin, cache);
                }
                pluginActions.add(new PluginActionWrapper(pluginAction, cache));
                pluginActionIndicies.add(pluginActions.size() - 1);
            }
        }
        return pluginActionIndicies;
    }

    @AllArgsConstructor
    private static class PluginActionWrapper {
        HoopoePluginAction pluginAction;
        HoopoeThreadLocalCache cache;
    }

    //todo implement cleanup of long-running data
    private static class ThreadProfileData {
        private long startTimeInNs;
        private boolean terminated;
        private List<TraceNode> traceNodes = new LinkedList<>();

        private ThreadProfileData(long startTimeInNs) {
            this.startTimeInNs = startTimeInNs;
        }
    }

}