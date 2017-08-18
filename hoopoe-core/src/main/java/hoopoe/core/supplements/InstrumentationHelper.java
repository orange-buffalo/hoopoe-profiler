package hoopoe.core.supplements;

import hoopoe.core.HoopoeProfilerImpl;
import hoopoe.core.ClassMetadataReader;
import hoopoe.core.components.PluginManager;
import hoopoe.core.bootstrap.HoopoeProfilerBridge;
import hoopoe.core.configuration.Configuration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.LongConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.io.IOUtils;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;

@Slf4j(topic = "hoopoe.profiler")
public class InstrumentationHelper {

    private Collection<Pattern> excludedClassesPatterns;
    private Collection<Pattern> includedClassesPatterns;
    private HoopoeProfilerImpl profiler;
    
    private Instrumentation instrumentation;
    private Collection<ClassFileTransformer> transformers = new ArrayList<>(2);

    private Configuration configuration;
    private PluginManager pluginManager;
    private ClassMetadataReader classMetadataReader;

    public InstrumentationHelper(Configuration configuration, PluginManager pluginManager, ClassMetadataReader classMetadataReader) {
        this.excludedClassesPatterns = excludedClassesPatterns;
        this.includedClassesPatterns = includedClassesPatterns;
        this.profiler = profiler;

        this.pluginManager = pluginManager;
        this.configuration = configuration;
        this.classMetadataReader = classMetadataReader;
    }

    public void createClassFileTransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        deployBootstrapJar(instrumentation);
        initProfilerBridge();

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
                                        profiler.getConfiguration().getMinimumTrackedInvocationTimeInNs()));

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

    private void deployBootstrapJar(Instrumentation instrumentation) {
        try {
            Path bootstrapJarDir = Files.createTempDirectory("hoopoe-bootstrap-");
            File bootstrapJar = new File(bootstrapJarDir.toFile(), "hoopoe-bootstrap.jar");
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("hoopoe-bootstrap.jar");
            if (resourceAsStream == null) {
                throw new IllegalStateException("no bootstrap");
            }
            IOUtils.copy(resourceAsStream,
                    new FileOutputStream(bootstrapJar));
            log.info("generated profiler bridge jar: {}", bootstrapJar);
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar));
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates delegator via ByteBuddy.
     * In case of anonymous / inner classes HoopoeProfilerBridge is requested during this class loading,
     * and it is not available as bootstrap jar is not yet deployed.
     */
    private void initProfilerBridge() {
        try {
            HoopoeProfilerBridge.instance = new ByteBuddy()
                    .subclass(HoopoeProfilerBridge.class)
                    .method(isAbstract())
                    .intercept(MethodDelegation.to(profiler))
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        log.info("profiler bridge initialized");
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

            if (HoopoeProfilerBridge.enabled && HoopoeProfilerBridge.profilingStartTime <= startTime) {
                // if plugin is attached to method, always report it
                HoopoeProfilerBridge.instance.profileCall(
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

            if (HoopoeProfilerBridge.enabled && HoopoeProfilerBridge.profilingStartTime <= startTime) {
                long endTime = System.nanoTime();

                if (endTime - startTime >= minimumTrackedTimeInNs) {
                    HoopoeProfilerBridge.instance.profileCall(
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

            if (HoopoeProfilerBridge.enabled && HoopoeProfilerBridge.profilingStartTime <= startTime) {
                long endTime = System.nanoTime();

                if (endTime - startTime >= minimumTrackedTimeInNs) {
                    HoopoeProfilerBridge.instance.profileCall(
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