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
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import static net.bytebuddy.matcher.ElementMatchers.any;
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

    public InstrumentationHelper(Collection<Pattern> excludedClassesPatterns, HoopoeProfilerImpl profiler) {
        this.excludedClassesPatterns = excludedClassesPatterns;
        this.profiler = profiler;
    }

    public ClassFileTransformer createClassFileTransformer(Instrumentation instrumentation) {
        deployBootstrapJar(instrumentation);
        intiProfilerBridge();

        Advice.WithCustomMapping baseAdviceConfig = Advice
                .withCustomMapping()
                .bind(MethodSignature.class, new MethodSignatureValue())
                .bind(ClassName.class, new ClassNameValue())
                .bind(MinimumTrackedTime.class, new MinimumTrackedTimeValue(
                        profiler.getConfiguration().getMinimumTrackedInvocationTimeInNs()));

        return new AgentBuilder.Default()
                .with(new AgentListener())
                .type(new ExcludedClassesMatcher())
                .transform((builder, typeDescription, classLoader) -> {
                    if (classLoader != null && classLoader.getClass().getName().equals("hoopoe.utils.HoopoeClassLoader")) {
                        return builder;
                    }
                    return builder
                            .visit(baseAdviceConfig
                                    .bind(PluginActions.class, new PluginActionsValue())
                                    .to(PluginsAwareAdvice.class)
                                    .on(new PluginsAwareMethodsMatcher())
                            )
                            .visit(baseAdviceConfig
                                    .to(PluginsUnawareAdvice.class)
                                    .on(new PluginsUnawareMethodsMatcher())
                            )
                            .visit(baseAdviceConfig
                                    .to(PluginsUnawareConstructorAdvice.class)
                                    .on(new PluginsUnawareConstructorMethodsMatcher())
                            );
                })
                .installOn(instrumentation);
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
    private void intiProfilerBridge() {
        try {
            HoopoeProfilerBridge.instance = new ByteBuddy()
                    .subclass(HoopoeProfilerBridge.class)
                    .method(any())
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
                interfaceDescription -> superclasses.add(getClassName(interfaceDescription))
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MinimumTrackedTime {
    }

    private class MinimumTrackedTimeValue implements Advice.DynamicValue<MinimumTrackedTime> {

        private long time;

        private MinimumTrackedTimeValue(long time) {
            this.time = time;
        }

        @Override
        public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                              ParameterDescription.InDefinedShape target,
                              AnnotationDescription.Loadable<MinimumTrackedTime> annotation,
                              boolean initialized) {
            return time;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MethodSignature {
    }

    private static class MethodSignatureValue implements Advice.DynamicValue<MethodSignature> {
        @Override
        public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                              ParameterDescription.InDefinedShape target,
                              AnnotationDescription.Loadable<MethodSignature> annotation,
                              boolean initialized) {
            return getMethodSignature(instrumentedMethod);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface ClassName {
    }

    private static class ClassNameValue implements Advice.DynamicValue<ClassName> {
        @Override
        public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                              ParameterDescription.InDefinedShape target,
                              AnnotationDescription.Loadable<ClassName> annotation,
                              boolean initialized) {
            return getClassName(instrumentedMethod.getDeclaringType());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface PluginActions {
    }

    private class PluginActionsValue implements Advice.DynamicValue<PluginActions> {
        @Override
        public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                              ParameterDescription.InDefinedShape target,
                              AnnotationDescription.Loadable<PluginActions> annotation,
                              boolean initialized) {
            return getPluginActions(instrumentedMethod);
        }
    }

    private class ExcludedClassesMatcher implements ElementMatcher<TypeDescription> {
        @Override
        public boolean matches(TypeDescription target) {
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
    }

    private static class PluginsAwareAdvice {

        @Advice.OnMethodEnter
        public static long before() {
            HoopoeProfilerBridge.callStackDepth.set(HoopoeProfilerBridge.callStackDepth.get() + 1);
            return System.nanoTime();
        }

        @Advice.OnMethodExit
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs,
                                 @Advice.BoxedArguments Object[] arguments,
                                 @Advice.This(optional = true) Object thisInMethod,
                                 @Advice.BoxedReturn Object returnValue,
                                 @PluginActions PluginActionIndicies pluginActionIndicies) throws Exception {
            long endTime = System.nanoTime();

            Integer depth = HoopoeProfilerBridge.callStackDepth.get() - 1;
            HoopoeProfilerBridge.callStackDepth.set(depth);

            if (endTime - startTime >= minimumTrackedTimeInNs) {
                HoopoeProfilerBridge.instance.profileCall(
                        startTime, endTime, className, methodSignature,
                        pluginActionIndicies.getIds(), arguments, returnValue, thisInMethod
                );
            }

            if (depth == 0) {
                HoopoeProfilerBridge.instance.finishThreadProfiling();
            }
        }
    }

    private static class PluginsUnawareAdvice {

        @Advice.OnMethodEnter
        public static long before() {
            HoopoeProfilerBridge.callStackDepth.set(HoopoeProfilerBridge.callStackDepth.get() + 1);
            return System.nanoTime();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs) throws Exception {
            long endTime = System.nanoTime();

            Integer depth = HoopoeProfilerBridge.callStackDepth.get() - 1;
            HoopoeProfilerBridge.callStackDepth.set(depth);

            if (endTime - startTime >= minimumTrackedTimeInNs) {
                HoopoeProfilerBridge.instance.profileCall(
                        startTime, endTime, className, methodSignature, null, null, null, null
                );
            }

            if (depth == 0) {
                HoopoeProfilerBridge.instance.finishThreadProfiling();
            }
        }
    }

    private static class PluginsUnawareConstructorAdvice {

        @Advice.OnMethodEnter
        public static long before() {
            HoopoeProfilerBridge.callStackDepth.set(HoopoeProfilerBridge.callStackDepth.get() + 1);
            return System.nanoTime();
        }

        @Advice.OnMethodExit
        public static void after(@Advice.Enter long startTime,
                                 @ClassName String className,
                                 @MethodSignature String methodSignature,
                                 @MinimumTrackedTime long minimumTrackedTimeInNs) throws Exception {
            long endTime = System.nanoTime();

            Integer depth = HoopoeProfilerBridge.callStackDepth.get() - 1;
            HoopoeProfilerBridge.callStackDepth.set(depth);

            if (endTime - startTime >= minimumTrackedTimeInNs) {
                HoopoeProfilerBridge.instance.profileCall(
                        startTime, endTime, className, methodSignature, null, null, null, null
                );
            }

            if (depth == 0) {
                HoopoeProfilerBridge.instance.finishThreadProfiling();
            }
        }
    }

    private class PluginsAwareMethodsMatcher implements ElementMatcher<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                    && !target.isTypeInitializer() && getPluginActions(target) != null;
        }
    }

    private class PluginsUnawareMethodsMatcher implements ElementMatcher<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return !target.isConstructor() && !target.isNative() && !target.isAbstract()
                    && !target.isTypeInitializer() && getPluginActions(target) == null;
        }
    }

    private class PluginsUnawareConstructorMethodsMatcher implements ElementMatcher<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return target.isConstructor() && !target.isNative() && !target.isAbstract()
                    && !target.isTypeInitializer() && getPluginActions(target) == null;
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
