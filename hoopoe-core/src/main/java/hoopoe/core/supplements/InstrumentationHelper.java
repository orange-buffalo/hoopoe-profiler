package hoopoe.core.supplements;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.configuration.HoopoeConfiguration;
import hoopoe.core.ClassMetadataReader;
import hoopoe.core.HoopoeProfilerFacade;
import hoopoe.core.components.PluginsManager;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.LongConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.utility.JavaModule;

@Slf4j(topic = "hoopoe.profiler")
public class InstrumentationHelper {

    private Collection<Pattern> excludedClassesPatterns;
    private Collection<Pattern> includedClassesPatterns;

    private Instrumentation instrumentation;
    private Collection<ClassFileTransformer> transformers = new ArrayList<>(2);

    private PluginsManager pluginManager;
    private ClassMetadataReader classMetadataReader;

    public InstrumentationHelper(
            PluginsManager pluginManager,
            ClassMetadataReader classMetadataReader) {

        this.pluginManager = pluginManager;
        this.classMetadataReader = classMetadataReader;
    }

    public void createClassFileTransformer(Instrumentation instrumentation, HoopoeProfiler profiler) {
        HoopoeConfiguration configuration = profiler.getConfiguration();
        this.excludedClassesPatterns = configuration.getExcludedClassesPatterns();
        this.includedClassesPatterns = configuration.getIncludedClassesPatterns();
        this.instrumentation = instrumentation;

        AgentBuilder baseAgentConfig = new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new AgentListener());

        Advice.WithCustomMapping baseAdviceConfig = Advice
                .withCustomMapping()

                .bind(MethodSignature.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                new TextConstant(classMetadataReader.getMethodSignature(instrumentedMethod)))

                .bind(ClassName.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                new TextConstant(classMetadataReader.getClassName(instrumentedMethod.getDeclaringType())))

                .bind(MinimumTrackedTime.class,
                        (instrumentedType, instrumentedMethod, target, annotation, assigner, initialized) ->
                                LongConstant.forValue(
                                        configuration.getMinimumTrackedInvocationTimeInNs()));

        transformers.add(baseAgentConfig
                .type(this::matchesProfiledClass)
                .transform((builder, typeDescription, classLoader, module) -> {
                    if (classLoader != null && classLoader.getClass().getName().equals("hoopoe.utils.HoopoeClassLoader")) {
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MinimumTrackedTime {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MethodSignature {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ClassName {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface PluginActions {
    }

    private static class EnterAdvice {

        @Advice.OnMethodEnter
        public static long before() {
            return System.nanoTime();
        }
    }

    private static class PluginsAwareAdvice {

        @Advice.OnMethodExit
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs,
                                 @Advice.AllArguments Object[] arguments,
                                 @Advice.This(optional = true) Object thisInMethod,
                                 @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
                                 @PluginActions Object pluginActionIndicies) throws Exception {

            if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
                // if plugin is attached to method, always report it
                HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                        startTime, System.nanoTime(), className, methodSignature,
                        pluginActionIndicies, arguments, returnValue, thisInMethod
                );
            }
        }
    }

    private static class PluginsUnawareAdvice {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs) throws Exception {

            if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
                long endTime = System.nanoTime();

                if (endTime - startTime >= minimumTrackedTimeInNs) {
                    HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                            startTime, endTime, className, methodSignature, null, null, null, null
                    );
                }
            }
        }
    }

    private static class ConstructorAdvice {

        @Advice.OnMethodExit
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs) throws Exception {

            if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
                long endTime = System.nanoTime();

                if (endTime - startTime >= minimumTrackedTimeInNs) {
                    HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                            startTime, endTime, className, methodSignature, null, null, null, null
                    );
                }
            }
        }
    }

    private static class AgentListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader,
                              JavaModule module, boolean loaded) {
            if (log.isTraceEnabled()) {
                log.trace("{} is skipped", typeDescription.getName());
            }
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader,
                            JavaModule module, boolean loaded, Throwable throwable) {
            log.debug("error while transforming {}: {}", typeName, throwable.getMessage());
        }

        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
            log.trace("{} is instrumented", typeName);
        }
    }

}