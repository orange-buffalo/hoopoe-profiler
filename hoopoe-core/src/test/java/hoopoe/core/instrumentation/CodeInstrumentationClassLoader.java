package hoopoe.core.instrumentation;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

class CodeInstrumentationClassLoader extends ClassLoader {

    private final byte[] classBytes;
    private final String className;

    static {
        registerAsParallelCapable();
    }

    public CodeInstrumentationClassLoader(Class clazz) throws IOException {
        super(CodeInstrumentationClassLoader.class.getClassLoader());

        this.className = clazz.getTypeName();
        try (InputStream classStream = getParent().getResourceAsStream(classNameToClassPath(this.className))) {
            this.classBytes = IOUtils.toByteArray(classStream);
        }
    }

    private static String classNameToClassPath(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null && getParent() != null) {
                ClassLoader parent = getParent();

                byte[] classBytes = null;

                if (className.equals(name)) {
                    classBytes = this.classBytes;
                }

                if (classBytes == null) {
                    clazz = parent.loadClass(name);
                } else {
                    clazz = defineClass(name, classBytes, 0, classBytes.length);
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
