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
import hoopoe.api.HoopoeTracer;
import hoopoe.core.supplements.ConfigurationHelper;
import hoopoe.core.supplements.HoopoeThreadLocalCacheImpl;
import hoopoe.core.supplements.InstrumentationHelper;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    private static final Collection<HoopoeAttribute> NO_ATTRIBUTES = Collections.emptyList();

    private static HoopoeProfilerImpl instance;

    private Thread mainThread;

    /**
     * A hook for tests. Otherwise it is extremely hard to disable profiler after test execution.
     */
    private ClassFileTransformer classFileTransformer;  // todo implement unload in this class?

    private HoopoeProfilerStorage storage;

    @Getter
    private HoopoeConfiguration configuration;

    private HoopoeTracer tracer;

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

        // todo cover with test
        tracer = configuration.createTracer();
        tracer.setupProfiler(this);

        instance = this;
        mainThread = Thread.currentThread();

        Collection<Pattern> excludedClassesPatterns = prepareExcludedClassesPatterns();
        InstrumentationHelper instrumentationHelper = new InstrumentationHelper(excludedClassesPatterns, this);
        classFileTransformer = instrumentationHelper.createClassFileTransformer(this, instrumentation);
    }

    public static void startMethodProfiling(String className,
                                            String methodSignature) {
        if (Thread.currentThread() == instance.mainThread) {    // todo cover with test
            return;
        }
        instance.tracer.onMethodEnter(className, methodSignature);
    }

    public static void finishMethodProfiling(int[] pluginActionIndicies,
                                             Object[] args,
                                             Object returnValue,
                                             Object thisInMethod) {
        Thread currentThread = Thread.currentThread();
        if (currentThread == instance.mainThread) {
            return;
        }

        long startTimeInNs = System.nanoTime();
        Collection<HoopoeAttribute> attributes = NO_ATTRIBUTES;
        if (pluginActionIndicies.length != 0) {
            attributes = new ArrayList<>(pluginActionIndicies.length);

            for (int pluginActionIndex : pluginActionIndicies) {
                PluginActionWrapper pluginActionWrapper = instance.pluginActions.get(pluginActionIndex);
                attributes.addAll(
                        pluginActionWrapper.pluginAction.getAttributes(
                                args, returnValue, thisInMethod, pluginActionWrapper.cache));
            }
        }

        HoopoeProfiledInvocation profiledInvocation =
                instance.tracer.onMethodLeave(attributes, System.nanoTime() - startTimeInNs);
        if (profiledInvocation != null) {
            instance.storage.addInvocation(currentThread, profiledInvocation);
            for (PluginActionWrapper pluginActionWrapper : instance.pluginActions) {
                pluginActionWrapper.cache.clear();
            }
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

    private Collection<Pattern> prepareExcludedClassesPatterns() {
        // todo revise, delegate to configuration
        Collection<Pattern> excludedClassesPatterns = new ArrayList<>();
        excludedClassesPatterns.add(Pattern.compile("hoopoe\\.core\\..*"));
        excludedClassesPatterns.add(Pattern.compile("hoopoe\\.api\\..*"));
        excludedClassesPatterns.add(Pattern.compile("javassist\\..*"));
        excludedClassesPatterns.add(Pattern.compile("sun\\..*"));
        excludedClassesPatterns.add(Pattern.compile("java\\.lang\\.reflect\\..*"));
        excludedClassesPatterns.add(Pattern.compile("java\\.lang\\..*"));
        excludedClassesPatterns.add(Pattern.compile("java\\.util\\..*"));
        excludedClassesPatterns.add(Pattern.compile("org\\.mockito\\..*"));
        excludedClassesPatterns.add(Pattern.compile("org\\.hamcrest\\..*"));
        excludedClassesPatterns.add(Pattern.compile("java\\.time\\..*"));
        excludedClassesPatterns.add(Pattern.compile("java\\..*"));
//        excludedClassesPatterns.add(Pattern.compile("java\\.lang\\..*"));
//        excludedClassesPatterns.add(Pattern.compile("java\\.io\\..*"));
//        excludedClassesPatterns.add(Pattern.compile("java\\.util\\..*"));
//        excludedClassesPatterns.add(Pattern.compile("org\\.gradle\\..*"));
        return excludedClassesPatterns;
    }

    @AllArgsConstructor
    private static class PluginActionWrapper {
        HoopoePluginAction pluginAction;
        HoopoeThreadLocalCache cache;
    }

}