package hoopoe.core;

import hoopoe.api.HoopoeAttribute;
import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoeMethodInfo;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginAction;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import hoopoe.api.HoopoeProfilerExtensionsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeThreadLocalCache;
import hoopoe.core.supplements.ConfigurationHelper;
import hoopoe.core.supplements.HoopoeThreadLocalCacheImpl;
import hoopoe.core.supplements.InstrumentationHelper;
import hoopoe.core.supplements.TraceNode;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    private static final ThreadLocal<ThreadProfileData> threadProfileDataHolder = new ThreadLocal<>();

    private static final ThreadLocal<Stack<ThreadProfileData>> threadProfileDataStack =
            ThreadLocal.withInitial(Stack::new);

    @Getter
    private HoopoeProfilerStorage storage;

    @Getter
    private HoopoeConfiguration configuration;

    private List<PluginActionWrapper> pluginActions = new CopyOnWriteArrayList<>();

    private Collection<HoopoePlugin> plugins;

    private Map<HoopoePlugin, HoopoeThreadLocalCache> pluginsThreadLocalCaches = new HashMap<>();

    private InstrumentationHelper instrumentationHelper;

    public HoopoeProfilerImpl(String rawArgs, Instrumentation instrumentation) {
        log.info("starting profiler");

        ConfigurationHelper configurationHelper = new ConfigurationHelper();
        configuration = configurationHelper.getConfiguration(rawArgs);
        storage = configuration.createProfilerStorage();

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

    public void onRunnableEnter() {
        threadProfileDataStack.get().push(threadProfileDataHolder.get());
        threadProfileDataHolder.set(null);
    }

    public void onRunnableExit() {
        ThreadProfileData profiledData = threadProfileDataHolder.get();
        if (profiledData != null) {

            // nodes are naturally organized in a way, that child node is always before its parent node
            List<TraceNode> traceNodes = profiledData.traceNodes;
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
        }

        // todo remove long-running data, as it is probably a thread-pool code
        threadProfileDataHolder.set(threadProfileDataStack.get().pop());
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