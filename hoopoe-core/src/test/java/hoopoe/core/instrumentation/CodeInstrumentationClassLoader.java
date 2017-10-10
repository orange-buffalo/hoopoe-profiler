package hoopoe.core.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

class CodeInstrumentationClassLoader extends ClassLoader {

    private final Map<String, byte[]> classesBytes = new HashMap<>();

    static {
        registerAsParallelCapable();
    }

    public CodeInstrumentationClassLoader(Class... classes) throws IOException {
        super(CodeInstrumentationClassLoader.class.getClassLoader());

        for (Class clazz : classes) {
            String className = clazz.getTypeName();
            byte[] classBytes;
            try (InputStream classStream = getParent().getResourceAsStream(classNameToClassPath(className))) {
                classBytes = IOUtils.toByteArray(classStream);
            }
            classesBytes.put(className, classBytes);
        }
    }

    private static String classNameToClassPath(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            ClassLoader parent = getParent();
            if (clazz == null) {
                if (classesBytes.containsKey(name)) {
                    byte[] classBytes = classesBytes.get(name);
                    clazz = defineClass(name, classBytes, 0, classBytes.length);

                } else if (parent != null) {
                    clazz = parent.loadClass(name);
                }
            }

            if (clazz == null) {
                throw new ClassNotFoundException(name);
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }
}
