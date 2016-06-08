package hoopoe.test.supplements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.io.IOUtils;

public class TestClassLoader extends ClassLoader {

    private Collection<String> packagesToInclude;

    static {
        registerAsParallelCapable();
    }

    public TestClassLoader(String... packagesToInclude) throws IOException {
        super(TestClassLoader.class.getClassLoader());
        this.packagesToInclude = Arrays.asList(packagesToInclude);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null && getParent() != null) {
                ClassLoader parent = getParent();
                if (isIncluded(name)) {
                    InputStream stream = parent.getResourceAsStream(name.replaceAll("\\.", "/") + ".class");
                    if (stream != null) {
                        try {
                            byte[] bytes = IOUtils.toByteArray(stream);
                            clazz = defineClass(name, bytes, 0, bytes.length);
                        }
                        catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }

                    }
                }

                if (clazz == null) {
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

    private boolean isIncluded(String name) {
        if (name.startsWith("java.lang")) {
            return false;
        }

        if (packagesToInclude.isEmpty()) {
            return true;
        }

        for (String packageToInclude : packagesToInclude) {
            if (name.startsWith(packageToInclude)) {
                return true;
            }
        }

        return false;
    }

}
