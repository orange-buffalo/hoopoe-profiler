package hoopoe.core;

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
    public void bootstrapHoopoe(String agentArgs, Instrumentation instrumentation) {
        JavaAgentArguments javaAgentArguments = new JavaAgentArguments(agentArgs);
        Environment environment = new Environment();

        Configuration configuration = ConfigurationFactory.createConfiguration(environment);
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();

        PluginManager pluginManager = new PluginManager(configuration, classMetadataReader);


        InstrumentationHelper instrumentationHelper = new InstrumentationHelper(configuration, pluginManager, classMetadataReader);

        ProfiledResultHelper profiledResultHelper = new ProfiledResultHelper();
        HoopoeProfilerImpl profiler = new HoopoeProfilerImpl(configuration, pluginManager, instrumentationHelper, profiledResultHelper);

        
        profiler.instrument(instrumentation);

    }

}
