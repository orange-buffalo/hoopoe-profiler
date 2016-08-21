package hoopoe.core.supplements;

import hoopoe.api.HoopoeMethodInfo;
import hoopoe.core.HoopoeProfilerImpl;
import hoopoe.core.bootstrap.HoopoeProfilerBridge;
import hoopoe.core.bootstrap.PluginActionIndicies;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "hoopoe.profiler")
public class InstrumentationHelper {

    private Collection<Pattern> excludedClassesPatterns;
    private HoopoeProfilerImpl profiler;
    // todo multithreading
    // todo cleanup
    private Map<String, PluginActionIndicies> pluginActionsCache = new HashMap<>();
    private Instrumentation instrumentation;
    private Collection<ClassFileTransformer> transformers = new ArrayList<>(2);

    public InstrumentationHelper(Collection<Pattern> excludedClassesPatterns, HoopoeProfilerImpl profiler) {
        this.excludedClassesPatterns = excludedClassesPatterns;
        this.profiler = profiler;
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

                .bind(MethodSignature.class, (instrumentedMethod, target, annotation, initialized) ->
                        getMethodSignature(instrumentedMethod))

                .bind(ClassName.class, (instrumentedMethod, target, annotation, initialized) ->
                        getClassName(instrumentedMethod.getDeclaringType()))

                .bind(MinimumTrackedTime.class, (instrumentedMethod, target, annotation, initialized) ->
                        profiler.getConfiguration().getMinimumTrackedInvocationTimeInNs());

        transformers.add(baseAgentConfig
                .type(this::matchesProfiledClass)
                .transform((builder, typeDescription, classLoader) -> {
                    if (classLoader != null && classLoader.getClass().getName().equals("hoopoe.utils.HoopoeClassLoader")) {
                        return builder;
                    }
                    return builder
                            .visit(baseAdviceConfig
                                    .bind(PluginActions.class, (instrumentedMethod, target, annotation, initialized) ->
                                            getPluginActions(instrumentedMethod))
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

    private PluginActionIndicies getPluginActions(MethodDescription method) {
        TypeDefinition declaringType = method.getDeclaringType();
        String className = getClassName(declaringType);
        String methodSignature = getMethodSignature(method);

        String methodKey = className + methodSignature;
        if (pluginActionsCache.containsKey(methodKey)) {
            return pluginActionsCache.get(methodKey);
        }

        HoopoeMethodInfo methodInfo = new HoopoeMethodInfo(
                className,
                methodSignature,
                getSuperclasses(declaringType));

        List<Integer> rawPluginActionIndicies = profiler.addPluginActions(methodInfo);
        PluginActionIndicies pluginActionIndicies = rawPluginActionIndicies.isEmpty()
                ? null
                : new PluginActionIndicies(ArrayUtils.toPrimitive(
                rawPluginActionIndicies.toArray(new Integer[rawPluginActionIndicies.size()])));
        pluginActionsCache.put(methodKey, pluginActionIndicies);
        return pluginActionIndicies;
    }

    private static Collection<String> getSuperclasses(TypeDefinition classDescription) {
        Set<String> superclasses = new HashSet<>();
        collectSuperclasses(classDescription, superclasses);
        return Collections.unmodifiableSet(superclasses);
    }

    private static void collectSuperclasses(TypeDefinition classDescription, Set<String> superclasses) {
        classDescription.getInterfaces().asErasures().forEach(
                interfaceDescription -> {
                    superclasses.add(getClassName(interfaceDescription));
                    collectSuperclasses(interfaceDescription, superclasses);
                }
        );

        TypeDescription.Generic superClassGeneric = classDescription.getSuperClass();
        if (superClassGeneric != null) {
            TypeDefinition superClassDescription = superClassGeneric.asErasure();
            superclasses.add(getClassName(superClassDescription));
            collectSuperclasses(superClassDescription, superclasses);
        }
    }

    private static String getClassName(TypeDefinition classDescription) {
        return classDescription.getTypeName();
    }

    private static String getMethodSignature(MethodDescription methodDescription) {
        StringBuilder builder = new StringBuilder();
        if (methodDescription.isConstructor()) {
            TypeDefinition declaringType = methodDescription.getDeclaringType();
            if (declaringType instanceof TypeDescription) {
                builder.append(((TypeDescription) declaringType).getSimpleName());
            }
            else {
                String typeName = declaringType.getTypeName();
                builder.append(typeName.substring(typeName.lastIndexOf('.')));
            }
        }
        else {
            builder.append(methodDescription.getName());
        }
        builder.append('(');

        TypeList typeDescriptions = methodDescription.getParameters().asTypeList().asErasures();
        boolean first = true;
        for (TypeDescription next : typeDescriptions) {
            if (!first) {
                builder.append(',');
            }
            builder.append(next.getCanonicalName());
            first = false;
        }

        builder.append(')');
        return builder.toString();
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

        for (Pattern excludedClassesPattern : excludedClassesPatterns) {
            if (excludedClassesPattern.matcher(className).matches()) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesPluginAwareMethod(MethodDescription target) {
        return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                && !target.isTypeInitializer() && getPluginActions(target) != null;
    }

    private boolean matchesPluginUnawareMethod(MethodDescription target) {
        return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                && !target.isTypeInitializer() && getPluginActions(target) == null;
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
                                 @Advice.BoxedArguments Object[] arguments,
                                 @Advice.This(optional = true) Object thisInMethod,
                                 @Advice.BoxedReturn Object returnValue,
                                 @PluginActions PluginActionIndicies pluginActionIndicies) throws Exception {

            if (HoopoeProfilerBridge.enabled) {
                // if plugin is attached to method, always report it
                HoopoeProfilerBridge.instance.profileCall(
                        startTime, System.nanoTime(), className, methodSignature,
                        pluginActionIndicies.getIds(), arguments, returnValue, thisInMethod
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

            if (HoopoeProfilerBridge.enabled) {
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

            if (HoopoeProfilerBridge.enabled) {
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
        public void onIgnored(TypeDescription typeDescription,
                              ClassLoader classLoader,
                              JavaModule module) {
            if (log.isTraceEnabled()) {
                log.trace("{} is skipped", typeDescription.getName());
            }
        }

        @Override
        public void onError(String typeName,
                            ClassLoader classLoader,
                            JavaModule module,
                            Throwable throwable) {
            log.debug("error while transforming {}: {}", typeName, throwable.getMessage());
        }

        @Override
        public void onComplete(String typeName,
                               ClassLoader classLoader,
                               JavaModule module) {
            log.trace("{} is instrumented", typeName);
        }
    }

}
