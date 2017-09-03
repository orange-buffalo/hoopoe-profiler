package hoopoe.core;

import hoopoe.core.components.ComponentLoader;
import hoopoe.core.components.ExtensionsManager;
import hoopoe.core.components.PluginsManager;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.configuration.ConfigurationFactory;
import hoopoe.core.supplements.InstrumentationHelper;
import hoopoe.core.supplements.ProfiledResultHelper;
import java.lang.instrument.Instrumentation;

/**
 * Initializes the profiler and all related components.
 * <p>
 * Performs duties of dependency injection, decoupling components and externalizing dependencies initialization.
 */
public class HoopoeBootstrapper {

    /**
     * Entry point for bootstrap process. Must be called by Java Agent.
     *
     * @param agentArgs       additional parameters supplied for initialization; in java agent notation.
     * @param instrumentation instrumentation to use to apply profiling code with.
     */
    public static HoopoeProfilerImpl bootstrapHoopoe(
            String agentArgs,
            Instrumentation instrumentation) {
        Environment environment = new Environment(agentArgs);

        Configuration configuration = ConfigurationFactory.createConfiguration(environment);
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        ClassLoader currentClassLoader = HoopoeBootstrapper.class.getClassLoader();
        ComponentLoader componentLoader = new ComponentLoader(currentClassLoader);

        PluginsManager pluginManager = new PluginsManager(configuration, componentLoader, classMetadataReader);
        ExtensionsManager extensionsManager = new ExtensionsManager(configuration, componentLoader);

        InstrumentationHelper instrumentationHelper = new InstrumentationHelper(pluginManager,
                classMetadataReader);

        ProfiledResultHelper profiledResultHelper = new ProfiledResultHelper();

        HoopoeProfilerImpl profiler = HoopoeProfilerImpl.builder()
                .configuration(configuration)
                .pluginsManager(pluginManager)
                .extensionsManager(extensionsManager)
                .instrumentationHelper(instrumentationHelper)
                .profiledResultHelper(profiledResultHelper)
                .build();

        profiler.instrument(instrumentation);

        return profiler;
    }
}
