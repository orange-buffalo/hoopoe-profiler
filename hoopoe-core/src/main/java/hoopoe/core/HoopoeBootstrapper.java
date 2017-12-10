package hoopoe.core;

import hoopoe.core.components.ComponentLoader;
import hoopoe.core.components.ExtensionsManager;
import hoopoe.core.components.PluginsManager;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.configuration.ConfigurationFactory;
import hoopoe.core.instrumentation.ClassMetadataReader;
import hoopoe.core.instrumentation.CodeInstrumentation;
import hoopoe.core.tracer.HotSpotCalculator;
import hoopoe.core.tracer.TraceNormalizer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes the profiler and all related components.
 * <p>
 * Performs duties of dependency injection, decoupling components and externalizing dependencies initialization.
 */
@Slf4j(topic = "hoopoe.profiler")
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

        deployFacadeJar(instrumentation);
        return initProfiler(agentArgs, instrumentation);
    }

    private static HoopoeProfilerImpl initProfiler(
            String agentArgs,
            Instrumentation instrumentation) {

        Environment environment = new Environment(agentArgs);

        Configuration configuration = ConfigurationFactory.createConfiguration(environment);
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        ClassLoader currentClassLoader = HoopoeBootstrapper.class.getClassLoader();
        ComponentLoader componentLoader = new ComponentLoader(currentClassLoader);

        PluginsManager pluginManager = new PluginsManager(configuration, componentLoader);
        ExtensionsManager extensionsManager = new ExtensionsManager(configuration, componentLoader);

        CodeInstrumentation codeInstrumentation = new CodeInstrumentation(
                pluginManager, classMetadataReader, configuration);

        TraceNormalizer traceNormalizer = new TraceNormalizer();

        HoopoeProfilerImpl profiler = HoopoeProfilerImpl.builder()
                .configuration(configuration)
                .pluginsManager(pluginManager)
                .extensionsManager(extensionsManager)
                .codeInstrumentation(codeInstrumentation)
                .traceNormalizer(traceNormalizer)
                .hotSpotCalculator(new HotSpotCalculator())
                .build();

        profiler.instrument(instrumentation);

        return profiler;
    }

    private static void deployFacadeJar(Instrumentation instrumentation) {
        try {
            Path tempDir = Files.createTempDirectory("hoopoe-");
            File facadeJar = new File(tempDir.toFile(), "hoopoe-facade.jar");
            try (InputStream resourceAsStream =
                         HoopoeBootstrapper.class.getClassLoader().getResourceAsStream("hoopoe-facade.jar")) {

                Files.copy(resourceAsStream, facadeJar.toPath());
                log.info("generated profiler facade jar: {}", facadeJar);
            }

            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(facadeJar));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
