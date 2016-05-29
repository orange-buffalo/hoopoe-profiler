package hoopoe.test.supplements;

import java.io.IOException;
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

public class TestClassLoader extends ClassLoader {

    private Map<String, byte[]> classesData = new HashMap<>();

    static {
        registerAsParallelCapable();
    }

    public TestClassLoader(String packageToAdd) throws NotFoundException, IOException, CannotCompileException {
        super(TestClassLoader.class.getClassLoader());

        try {
            Reflections reflections = new Reflections(packageToAdd, new SubTypesScanner(false));
            Set<Class<?>> guineaPigClasses = reflections.getSubTypesOf(Object.class);
            guineaPigClasses.size();
            ClassPool classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(TestClassLoader.class.getClassLoader()));
            for (Class guineaPigClass : guineaPigClasses) {
                CtClass ctClass = classPool.get(guineaPigClass.getCanonicalName());
                classesData.put(guineaPigClass.getCanonicalName(), ctClass.toBytecode());
            }
        }
        catch (IOException | CannotCompileException | NotFoundException e) {
            throw new IllegalStateException(e);
        }
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
