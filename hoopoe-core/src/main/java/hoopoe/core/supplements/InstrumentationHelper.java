package hoopoe.core.supplements;

import hoopoe.api.HoopoePlugin;
import hoopoe.core.HoopoeProfilerImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "hoopoe.profiler")
public class InstrumentationHelper {

    private static final String BRIDGE_CLASS_NAME = "hoopoe.core.HoopoeProfilerBridge";

    private static final String BEFORE_METHOD_CALL = MessageFormat.format(
            "{0}.startProfilingMethod.invoke(" +
                    "{0}.profiler, new java.lang.Object[] '{'\"%s\", \"%s\", $args'}');", BRIDGE_CLASS_NAME);

    private static final String AFTER_METHOD_CALL = MessageFormat.format(
            "{0}.finishProfilingMethod.invoke({0}.profiler, {0}.NO_ARGS);", BRIDGE_CLASS_NAME);

    private byte[] hoopoeProfilerBridgeClassBytes;

    private Collection<Pattern> excludedClassesPatterns;

    private ConcurrentMap<ClassLoader, ClassPool> classPools = new ConcurrentHashMap<>();

    private Collection<HoopoePlugin> plugins;

    public InstrumentationHelper(Collection<Pattern> excludedClassesPatterns,
                                 Collection<HoopoePlugin> plugins) {
        this.excludedClassesPatterns = excludedClassesPatterns;
        this.plugins = plugins;
    }

    public ClassFileTransformer createClassFileTransformer(HoopoeProfilerImpl profiler,
                                                           Instrumentation instrumentation) {
        createProfilerBridge(instrumentation, profiler);
        ClassFileTransformer classFileTransformer = InstrumentationHelper.this::transform;
        instrumentation.addTransformer(classFileTransformer);
        return classFileTransformer;
    }

    private void createProfilerBridge(Instrumentation instrumentation, HoopoeProfilerImpl profiler) {
        try {
            generateProfilerBridgeClass();
            File profilerBridgeJar = generateProfilerBridgeJar();
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(profilerBridgeJar));
            intiProfilerBridge(profiler);
        }
        catch (CannotCompileException | IOException | ReflectiveOperationException e) {
            log.error("cannot create the bridge", e);
            throw new IllegalArgumentException(e);
        }
    }

    private void generateProfilerBridgeClass() throws CannotCompileException, IOException {
        ClassPool classPool = ClassPool.getDefault();

        CtClass bridgeCtClass = classPool.makeClass(BRIDGE_CLASS_NAME);

        bridgeCtClass.addField(
                CtField.make("public static java.lang.Object profiler;", bridgeCtClass));

        bridgeCtClass.addField(
                CtField.make("public static java.lang.reflect.Method startProfilingMethod;", bridgeCtClass));

        bridgeCtClass.addField(
                CtField.make("public static java.lang.reflect.Method finishProfilingMethod;", bridgeCtClass));

        bridgeCtClass.addField(
                CtField.make("public static final java.lang.Object[] NO_ARGS = new java.lang.Object[0];", bridgeCtClass));

        hoopoeProfilerBridgeClassBytes = bridgeCtClass.toBytecode();

        bridgeCtClass.detach();
    }

    private File generateProfilerBridgeJar() throws IOException {
        Path bootstrapJarDir = Files.createTempDirectory("hoopoe-bootstrap-");
        File bootstrapJar = new File(bootstrapJarDir.toFile(), "hoopoe-profiler-bridge.jar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(bootstrapJar))) {
            jarOutputStream.putNextEntry(new ZipEntry(BRIDGE_CLASS_NAME.replaceAll("\\.", "/") + ".class"));
            IOUtils.write(hoopoeProfilerBridgeClassBytes, jarOutputStream);
            jarOutputStream.closeEntry();
        }
        log.info("generated profiler bridge jar: {}", bootstrapJar);
        return bootstrapJar;
    }

    private void intiProfilerBridge(HoopoeProfilerImpl profiler)
            throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {

        Class bridgeClass = Class.forName(BRIDGE_CLASS_NAME, true, null);

        bridgeClass.getField("profiler").set(null, profiler);

        Method[] profilerMethods = profiler.getClass().getMethods();

        Method startProfilingMethod = Arrays.stream(profilerMethods)
                .filter(method -> method.getName().equals("startMethodProfiling"))
                .findAny()
                .get();
        startProfilingMethod.setAccessible(true);
        bridgeClass.getField("startProfilingMethod").set(null, startProfilingMethod);

        Method finishProfilingMethod = Arrays.stream(profilerMethods)
                .filter(method -> method.getName().equals("finishMethodProfiling"))
                .findAny()
                .get();
        finishProfilingMethod.setAccessible(true);
        bridgeClass.getField("finishProfilingMethod").set(null, finishProfilingMethod);
    }

    private byte[] transform(ClassLoader loader,
                             String className,
                             Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain,
                             byte[] classfileBuffer) throws IllegalClassFormatException {

        String canonicalClassName = className.replaceAll("/", ".");

        log.debug("{} transformation requested", canonicalClassName);

        for (Pattern excludedClassesPattern : excludedClassesPatterns) {
            if (excludedClassesPattern.matcher(canonicalClassName).matches()) {
                log.debug("skipping, due to excluded pattern {}", excludedClassesPattern);
                return null;
            }
        }

        try {
            ClassPool classPool = getClassPool(loader);
            CtClass ctClass = classPool.get(canonicalClassName);

            if (!isLockedForInterception(ctClass)) {
                log.debug("modifying {}", canonicalClassName);

                Collection<String> superclasses = getSuperclasses(ctClass);

                Collection<CtBehavior> behaviors = new ArrayList<>();
                behaviors.addAll(Arrays.asList(ctClass.getDeclaredMethods()));
                behaviors.addAll(Arrays.asList(ctClass.getDeclaredConstructors()));

                for (CtBehavior behavior : behaviors) {
                    try {
                        if (!isLockedForInterception(behavior)) {
                            String methodName = getMethodName(ctClass, behavior.getLongName());

                            List<String> supportedPlugins = plugins.stream()
                                    .filter(hoopoePlugin -> hoopoePlugin.supports(ctClass.getName(), superclasses, methodName))
                                    .map(HoopoePlugin::getId)
                                    .collect(Collectors.toList());

                            behavior.insertBefore(String.format(BEFORE_METHOD_CALL, canonicalClassName, methodName));
                            behavior.insertAfter(AFTER_METHOD_CALL, true);
                        }
                    }
                    catch (CannotCompileException e) {
                        log.warn("cannot change body of {}: {}", behavior.getLongName(), e.getReason());
                    }
                }
                byte[] modifiedClassBytes = ctClass.toBytecode();
                ctClass.detach();
                return modifiedClassBytes;
            }
            else {
                log.debug("{} is not interceptable", canonicalClassName);
            }
        }
        catch (Exception e) {
            log.warn("cannot change class {}: {}", canonicalClassName, e.getMessage());
        }
        return null;
    }

    private ClassPool getClassPool(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader().getParent();
        }

        ClassPool classPool = classPools.get(classLoader);
        if (classPool == null) {
            log.info("new classloader involved: {}", classLoader);

            classPool = new ClassPool();
            ClassLoader classPathLoader = classLoader;
            while (classPathLoader != null) {
                classPool.appendClassPath(new LoaderClassPath(classPathLoader));
                classPathLoader = classPathLoader.getParent();
            }
            classPool.appendClassPath(
                    new ByteArrayClassPath(BRIDGE_CLASS_NAME, hoopoeProfilerBridgeClassBytes));

            classPools.putIfAbsent(classLoader, classPool);
        }
        return classPool;
    }

    private boolean isLockedForInterception(CtClass ctClass) {
        return ctClass.isFrozen() || ctClass.isAnnotation() || ctClass.isPrimitive() || ctClass.isInterface();
    }

    private Collection<String> getSuperclasses(CtClass ctClass) throws NotFoundException {
        Collection<String> superclasses = Arrays.stream(ctClass.getInterfaces())
                .map(CtClass::getName)
                .collect(Collectors.toSet());
        CtClass superclass = ctClass.getSuperclass();
        if (superclass != null) {
            superclasses.add(superclass.getName());
            superclasses.addAll(getSuperclasses(superclass));
        }
        return superclasses;
    }

    private boolean isLockedForInterception(CtBehavior ctBehavior) {
        int modifiers = ctBehavior.getModifiers();
        return Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers);
    }

    /**
     * for constructors longMethodName is: canonical class name + "([params])"
     * for methods longMethodName is: canonical class name + ".methodName([params])"
     * normalize both to "[methodName][simpleClassName]([parameters])"
     */
    private String getMethodName(CtClass ctClass, String longMethodName) {
        String shortMethodName = longMethodName.replace(ctClass.getName(), StringUtils.EMPTY);
        if (shortMethodName.startsWith(".")) {
            return shortMethodName.substring(1, shortMethodName.length());
        }
        else {
            return ctClass.getSimpleName() + shortMethodName;
        }
    }

}
