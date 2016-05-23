package hoopoe.core;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.core.supplements.ConfigurationHelper;
import hoopoe.core.supplements.InstrumentationHelper;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoopoe.profiler")
public class HoopoeProfilerImpl implements HoopoeProfiler {

    private static final ThreadLocal<HoopoeTraceNode> currentTraceNodeHolder = new ThreadLocal<>();

    private static HoopoeProfilerImpl instance;

    /**
     * A hook for tests. Otherwise it is extremely hard to disable profiler after test execution.
     */
    private ClassFileTransformer classFileTransformer;

    private HoopoeProfilerStorage storage;

    private HoopoeConfiguration configuration;

    private Map<String, HoopoePlugin> plugins;

    public HoopoeProfilerImpl(String rawArgs, Instrumentation instrumentation) {
        log.info("starting profiler");

        ConfigurationHelper configurationHelper = new ConfigurationHelper();
        configuration = configurationHelper.getConfiguration(rawArgs);
        storage = configuration.createProfilerStorage();

        HoopoePluginsProvider pluginsProvider = configuration.createPluginsProvider();
        pluginsProvider.setupProfiler(this);
        plugins = pluginsProvider.createPlugins().stream()
                .collect(Collectors.toMap(HoopoePlugin::getId, Function.identity()));

        Collection<Pattern> excludedClassesPatterns = prepareExcludedClassesPatterns();
        InstrumentationHelper instrumentationHelper =
                new InstrumentationHelper(excludedClassesPatterns, plugins.values());
        classFileTransformer = instrumentationHelper.createClassFileTransformer(this, instrumentation);

        instance = this;
    }

    public static void startMethodProfiling(String className,
                                            String[] superclasses,
                                            String methodSignature,
                                            String[] enabledPlugins,
                                            Object[] args) {
        HoopoeTraceNode previousTraceNode = currentTraceNodeHolder.get();
        HoopoeTraceNode currentTraceNode = new HoopoeTraceNode(previousTraceNode, className, methodSignature);
        for (String enabledPlugin : enabledPlugins) {
            HoopoePlugin plugin = instance.plugins.get(enabledPlugin);
            plugin.onCall(className, superclasses, methodSignature, args);
        }

        currentTraceNodeHolder.set(currentTraceNode);
    }

    public static void finishMethodProfiling() {
        HoopoeTraceNode currentTraceNode = currentTraceNodeHolder.get();
        currentTraceNode.finish();

        HoopoeTraceNode previousTraceNode = currentTraceNode.getParent();
        currentTraceNodeHolder.set(previousTraceNode);
        if (previousTraceNode == null) {
            instance.storage.consumeThreadTraceResults(Thread.currentThread(), currentTraceNode);
        }
    }

    private Collection<Pattern> prepareExcludedClassesPatterns() {
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

}