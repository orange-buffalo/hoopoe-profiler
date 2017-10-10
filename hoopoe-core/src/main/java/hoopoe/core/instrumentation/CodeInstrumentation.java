package hoopoe.core.instrumentation;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.components.PluginsManager;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

@Slf4j(topic = "hoopoe.profiler")
public class CodeInstrumentation {

    private Collection<Pattern> excludedClassesPatterns;
    private Collection<Pattern> includedClassesPatterns;
    private long minimumTrackedInvocationTimeInNs;

    private Instrumentation instrumentation;
    private ResettableClassFileTransformer transformer;

    private PluginsManager pluginManager;
    private ClassMetadataReader classMetadataReader;

    public CodeInstrumentation(
            PluginsManager pluginManager,
            ClassMetadataReader classMetadataReader,
            HoopoeConfiguration configuration) {

        this.pluginManager = pluginManager;
        this.classMetadataReader = classMetadataReader;
        this.excludedClassesPatterns = configuration.getExcludedClassesPatterns().stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        this.includedClassesPatterns = configuration.getIncludedClassesPatterns().stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        this.minimumTrackedInvocationTimeInNs = configuration.getMinimumTrackedInvocationTimeInNs();
    }

    public void createClassFileTransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.transformer = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new InstrumentationListener())
                .type(this::matchesProfiledClass)
                .transform((builder, typeDescription, classLoader, module) -> {
                    if (classLoader != null
                            && classLoader.getClass().getName().equals("hoopoe.utils.HoopoeClassLoader")) {
                        return builder;
                    }
                    return builder.visit(Advice
                            .withCustomMapping()
                            .bind(HoopoeAdvice.MinimumTrackedTime.class, minimumTrackedInvocationTimeInNs)
                            .bind(HoopoeAdvice.MethodSignature.class,
                                    (instrumentedType, instrumentedMethod, assigner, context) ->
                                            Advice.OffsetMapping.Target.ForStackManipulation.of(
                                                    classMetadataReader.getMethodSignature(instrumentedMethod)
                                            ))
                            .bind(HoopoeAdvice.PluginRecorders.class,
                                    (instrumentedType, instrumentedMethod, assigner, context) ->
                                            Advice.OffsetMapping.Target.ForStackManipulation.of(
                                                    pluginManager.getPluginRecordersReference(
                                                            classMetadataReader.createMethodInfo(instrumentedMethod))
                                            )
                            )
                            .to(HoopoeAdvice.class)
                            .on(this::nonNativeNonAbstractMethod)
                    );
                })
                .installOn(instrumentation);
    }

    /**
     * Resets the transformation on the classes.
     */
    void reset() {
        transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.REDEFINITION);
    }

    /**
     * Unloads the instrumentation so that further loaded classes are not affected.
     */
    void unload() {
        instrumentation.removeTransformer(transformer);
    }

    private boolean matchesProfiledClass(TypeDescription target) {
        String className = target.getName();

        if (className.startsWith("hoopoe.api")
                || className.startsWith("sun.") || className.startsWith("com.sun")
                || className.startsWith("jdk.")
                || className.contains("$$")) {
            return false;
        }

        for (Pattern includedClassesPattern : includedClassesPatterns) {
            if (includedClassesPattern.matcher(className).matches()) {
                return true;
            }
        }

        for (Pattern excludedClassesPattern : excludedClassesPatterns) {
            if (excludedClassesPattern.matcher(className).matches()) {
                return false;
            }
        }

        return true;
    }

    private boolean nonNativeNonAbstractMethod(MethodDescription target) {
        return !target.isNative() && !target.isAbstract();
    }
}