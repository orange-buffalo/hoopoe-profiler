package hoopoe.test.supplements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class TestClassLoader extends ClassLoader {

    private Collection<String> packagesToInclude = new ArrayList<>();
    private Map<String, byte[]> includedClasses = new HashMap<>();
    private boolean includeAllPackages;

    static {
        registerAsParallelCapable();
    }

    public TestClassLoader() {
        super(TestClassLoader.class.getClassLoader());
    }

    public void includePackages(String... packagesToInclude) {
        this.packagesToInclude.addAll(Arrays.asList(packagesToInclude));
    }

    public void includeClass(Class clazz) throws IOException {
        byte[] classBytes = IOUtils.toByteArray(
                getParent().getResourceAsStream(classNameToClassPath(clazz.getTypeName())));
        includeClass(clazz.getTypeName(), classBytes);
    }

    public void includeClass(String className, byte[] classBytes) {
        this.includedClasses.put(className, classBytes);
    }

    public void includeAllPackages() {
        this.includeAllPackages = true;
    }

    public static String classNameToClassPath(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null && getParent() != null) {
                ClassLoader parent = getParent();

                byte[] classBytes = null;

                if (includedClasses.containsKey(name)) {
                    classBytes = includedClasses.get(name);
                }
                else if (isIncludedByPackage(name)) {
                    InputStream stream = parent.getResourceAsStream(classNameToClassPath(name));
                    if (stream != null) {
                        try {
                            classBytes = IOUtils.toByteArray(stream);
                        }
                        catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }

                    }
                }

                if (classBytes == null) {
                    clazz = parent.loadClass(name);
                }
                else {
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

    private boolean isIncludedByPackage(String name) {
        if (name.startsWith("java.lang")) {
            return false;
        }

        if (includeAllPackages) {
            return true;
        }

        if (packagesToInclude.isEmpty()) {
            return false;
        }

        for (String packageToInclude : packagesToInclude) {
            if (name.startsWith(packageToInclude)) {
                return true;
            }
        }

        return false;
    }

}
