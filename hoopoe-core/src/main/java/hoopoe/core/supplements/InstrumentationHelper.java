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
import java.util.HashSet;
import java.util.List;
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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j(topic = "hoopoe.profiler")
public class InstrumentationHelper {

    private Collection<Pattern> excludedClassesPatterns;
    private HoopoeProfilerImpl profiler;

    public InstrumentationHelper(Collection<Pattern> excludedClassesPatterns, HoopoeProfilerImpl profiler) {
        this.excludedClassesPatterns = excludedClassesPatterns;
        this.profiler = profiler;
    }

    public ClassFileTransformer createClassFileTransformer(Instrumentation instrumentation) {
        deployBootstrapJar(instrumentation);
        intiProfilerBridge();

        return new AgentBuilder.Default()
                .with(new AgentListener())
                .type(
                        ElementMatchers.not(ElementMatchers.isInterface())
                                .and(new ExcludedClassesElementMatcher())
                )
                .transform(
                        (builder, typeDescription, classLoader) -> builder
                                .visit(Advice
                                        .withCustomMapping()
                                        .bind(MethodSignature.class, new MethodSignatureValue())
                                        .bind(ClassName.class, new ClassNameValue())
                                        .bind(PluginActions.class, new PluginActionsValue())
                                        .to(ProfilerBridgeAdvice.class)
                                        .on(new IncludedMethodElementMatcher())
                                )
                )
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
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(profiler))
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private PluginActionIndicies getPluginActions(String className,
                                                  Collection<String> superclasses,
                                                  String methodSignature) {
        HoopoeMethodInfo methodInfo = new HoopoeMethodInfo(className, methodSignature, superclasses);

        List<Integer> pluginActionIndicies = profiler.addPluginActions(methodInfo);
        return pluginActionIndicies.isEmpty()
                ? null
                : new PluginActionIndicies(ArrayUtils.toPrimitive(
                pluginActionIndicies.toArray(new Integer[pluginActionIndicies.size()])));
    }

    private static Collection<String> getSuperclasses(TypeDescription classDescription) {
        Set<String> superclasses = new HashSet<>();
        collectSuperclasses(classDescription, superclasses);
        return Collections.unmodifiableSet(superclasses);
    }

    private static void collectSuperclasses(TypeDescription classDescription, Set<String> superclasses) {
        classDescription.getInterfaces().asErasures().forEach(
                interfaceDescription -> superclasses.add(getClassName(interfaceDescription))
        );

        TypeDescription.Generic superClassGeneric = classDescription.getSuperClass();
        if (superClassGeneric != null) {
            TypeDescription superClassDescription = superClassGeneric.asErasure();
            superclasses.add(getClassName(superClassDescription));
            collectSuperclasses(superClassDescription, superclasses);
        }
    }

    private static String getClassName(TypeDescription classDescription) {
        return classDescription.getName();
    }

    private static String getMethodSignature(MethodDescription.InDefinedShape instrumentedMethod) {
        StringBuilder builder = new StringBuilder(
                instrumentedMethod.isConstructor()
                        ? instrumentedMethod.getDeclaringType().getSimpleName()
                        : instrumentedMethod.getName()
        ).append('(');

        TypeList typeDescriptions = instrumentedMethod.getParameters().asTypeList().asErasures();
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
            TypeDescription declaringType = instrumentedMethod.getDeclaringType();
            return getPluginActions(
                    getClassName(declaringType),
                    getSuperclasses(declaringType),
                    getMethodSignature(instrumentedMethod));
        }
    }

    private class ExcludedClassesElementMatcher implements ElementMatcher<TypeDescription> {
        @Override
        public boolean matches(TypeDescription target) {
            String className = target.getName();
            for (Pattern excludedClassesPattern : excludedClassesPatterns) {
                if (excludedClassesPattern.matcher(className).matches()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class ProfilerBridgeAdvice {

        @Advice.OnMethodEnter
        public static void before(@ClassName String className,
                                  @MethodSignature String methodSignature) throws Exception {
            HoopoeProfilerBridge.instance.startMethodProfiling(className, methodSignature);
//            System.out.print("");
        }

        @Advice.OnMethodExit
        public static void after(@Advice.BoxedArguments Object[] arguments,
                                 @Advice.This(optional = true) Object thisInMethod,
                                 @Advice.BoxedReturn Object returnValue,
                                 @PluginActions PluginActionIndicies indicies) throws Exception {
            HoopoeProfilerBridge.instance.finishMethodProfiling(
                    indicies == null ? HoopoeProfilerBridge.NO_PLUGINS : indicies.getIds(),
                    arguments,
                    returnValue,
                    thisInMethod);
//            System.out.print("");
        }

    }

    private static class IncludedMethodElementMatcher implements ElementMatcher<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return !target.isNative() && !target.isAbstract() && !target.isTypeInitializer();
        }
    }

    private static class AgentListener implements AgentBuilder.Listener {

        @Override
        public void onTransformation(TypeDescription typeDescription,
                                     ClassLoader classLoader,
                                     JavaModule module,
                                     DynamicType dynamicType) {
        }

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
            log.debug("error while transforming " + typeName, throwable);
        }

        @Override
        public void onComplete(String typeName,
                               ClassLoader classLoader,
                               JavaModule module) {
            log.trace("{} is instrumented", typeName);
        }
    }

}
