package hoopoe.core.instrumentation;

import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.ClassMetadataReader;
import hoopoe.core.components.PluginsManager;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.constant.LongConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;

@Slf4j(topic = "hoopoe.profiler")
public class CodeInstrumentation {

    private Collection<Pattern> excludedClassesPatterns;
    private Collection<Pattern> includedClassesPatterns;
    private long minimumTrackedInvocationTimeInNs;

    private Instrumentation instrumentation;
    private Collection<ClassFileTransformer> transformers = new ArrayList<>(2);

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

        AgentBuilder baseAgentConfig = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new InstrumentationListener());

        Advice.WithCustomMapping baseAdviceConfig = Advice
                .withCustomMapping()

                .bind(MethodSignature.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                new TextConstant(classMetadataReader.getMethodSignature(instrumentedMethod)))

                .bind(ClassName.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                new TextConstant(
                                        classMetadataReader.getClassName(instrumentedMethod.getDeclaringType())))

                .bind(MinimumTrackedTime.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                LongConstant.forValue(minimumTrackedInvocationTimeInNs));

        transformers.add(baseAgentConfig
                .type(this::matchesProfiledClass)
                .transform((builder, typeDescription, classLoader, module) -> {
                    if (classLoader != null && classLoader.getClass()
                            .getName()
                            .equals("hoopoe.utils.HoopoeClassLoader")) {
                        return builder;
                    }
                    return builder
                            .visit(baseAdviceConfig
                                    .bind(PluginActions.class,
                                            (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                                    pluginManager.getPluginActions(instrumentedMethod)
                                    )
                                    .to(EnterAdvice.class, PluginsAwareAdvice.class)
                                    .on(this::matchesPluginAwareMethod)
                            )
                            .visit(baseAdviceConfig
                                    .to(EnterAdvice.class, PluginsUnawareAdvice.class)
                                    .on(this::matchesPluginUnawareMethod)
                            )
                            .visit(baseAdviceConfig
                                    .to(EnterAdvice.class, ConstructorAdvice.class)
                                    .on(this::matchesConstructor)
                            );
                })
                .installOn(instrumentation));
    }

    public void unload() {
        transformers.forEach(transformer -> instrumentation.removeTransformer(transformer));
    }

    private boolean matchesProfiledClass(TypeDescription target) {
        if (target.isInterface()) {
            return false;
        }

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

    private boolean matchesPluginAwareMethod(MethodDescription target) {
        return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                && !target.isTypeInitializer() && pluginManager.getPluginActions(target) != null;
    }

    private boolean matchesPluginUnawareMethod(MethodDescription target) {
        return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                && !target.isTypeInitializer() && pluginManager.getPluginActions(target) == null;
    }

    private boolean matchesConstructor(MethodDescription target) {
        return target.isConstructor();
    }

}