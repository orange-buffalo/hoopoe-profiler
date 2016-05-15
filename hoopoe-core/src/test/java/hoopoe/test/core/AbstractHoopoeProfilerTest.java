package hoopoe.test.core;

import com.ea.agentloader.AgentLoader;
import hoopoe.core.HoopoeProfilerImpl;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public abstract class AbstractHoopoeProfilerTest {

    private static Map<String, byte[]> classesData = new HashMap<>();

    static {
        try {
            Reflections reflections = new Reflections("hoopoe.test.core.guineapigs", new SubTypesScanner(false));
            Set<Class<?>> guineaPigClasses = reflections.getSubTypesOf(Object.class);
            guineaPigClasses.size();
            ClassPool classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(AbstractHoopoeProfilerTest.class.getClassLoader()));
            for (Class guineaPigClass : guineaPigClasses) {
                CtClass ctClass = classPool.get(guineaPigClass.getCanonicalName());
                classesData.put(guineaPigClass.getCanonicalName(), ctClass.toBytecode());
            }
        }
        catch (IOException | CannotCompileException | NotFoundException e) {
            throw new IllegalStateException(e);
        }

    }

    protected void executeProfiling(Class entryPointClass, String entryPointMethod) {
        TestClassLoader classLoader;
        try {
            classLoader = new TestClassLoader();
        }
        catch (NotFoundException | IOException | CannotCompileException e) {
            throw new IllegalStateException(e);
        }

        TestAgent.load();

        try {
            String threadName = "testThread" + System.nanoTime();

            Class<?> instrumentedClass = classLoader.loadClass(entryPointClass.getCanonicalName());
            Object testObject = instrumentedClass.newInstance();
            Method instrumentedMethod = instrumentedClass.getDeclaredMethod(entryPointMethod);

            Thread thread = new Thread(() -> {
                try {
                    instrumentedMethod.invoke(testObject);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, threadName);
            thread.start();
            thread.join();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            TestAgent.unload();
        }
    }

    public static class TestAgent {

        private static Instrumentation instrumentation;
        private static ClassFileTransformer classFileTransformer;

        public static void load() {
            AgentLoader.loadAgentClass(TestAgent.class.getName(), null);
        }

        public static void agentmain(String args, Instrumentation instrumentation)
                throws NoSuchFieldException, IllegalAccessException {

            HoopoeProfilerImpl profiler = new HoopoeProfilerImpl(args, instrumentation);
            TestAgent.instrumentation = instrumentation;

            Field classFileTransformerField = HoopoeProfilerImpl.class.getDeclaredField("classFileTransformer");
            classFileTransformerField.setAccessible(true);
            classFileTransformer = (ClassFileTransformer) classFileTransformerField.get(profiler);
        }

        public static void unload() {
            instrumentation.removeTransformer(classFileTransformer);
        }
    }

    private static class TestClassLoader extends ClassLoader {

        static {
            registerAsParallelCapable();
        }

        public TestClassLoader() throws NotFoundException, IOException, CannotCompileException {
            super(TestClassLoader.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> clazz = findLoadedClass(name);
                if (clazz == null) {

                    byte[] classBytes = classesData.get(name);
                    if (classBytes != null) {
                        clazz = defineClass(name, classBytes, 0, classBytes.length);
                    }

                    if (clazz == null) {
                        ClassLoader parent = getParent();
                        if (parent != null) {
                            clazz = parent.loadClass(name);
                        }
                    }

                    if (clazz == null) {
                        throw new ClassNotFoundException(name);
                    }
                }

                if (resolve) {
                    resolveClass(clazz);
                }

                return clazz;
            }
        }
    }

}
