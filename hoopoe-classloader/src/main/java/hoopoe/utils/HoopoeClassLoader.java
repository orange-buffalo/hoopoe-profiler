package hoopoe.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class HoopoeClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    public static HoopoeClassLoader fromStream(InputStream inputStream,
                                               ClassLoader parentClassLoader) {

        Collection<URL> urls = new ArrayList<>();
        try (ZipInputStream stream = new ZipInputStream(inputStream)) {

            File rootDir = Files.createTempDirectory("hoopoe-cl-" + System.currentTimeMillis()).toFile();

            File classesDir = new File(rootDir, "classes");
            classesDir.mkdirs();
            urls.add(classesDir.toURI().toURL());

            ZipEntry nextEntry;
            while ((nextEntry = stream.getNextEntry()) != null) {
                if (!nextEntry.isDirectory()) {
                    String name = nextEntry.getName();
                    File targetFile = null;
                    if (name.startsWith("classes/")) {
                        targetFile = new File(classesDir, name.replaceFirst("classes", ""));
                    }
                    else if (name.startsWith("META-INF/")) {
                        targetFile = new File(classesDir, name);
                    }
                    else if (name.startsWith("lib/") && name.endsWith(".jar")) {
                        targetFile = new File(rootDir, name);
                        urls.add(targetFile.toURI().toURL());
                    }

                    if (targetFile != null) {
                        copyFile(stream, targetFile);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return new HoopoeClassLoader(urls.toArray(new URL[urls.size()]), parentClassLoader);
    }

    public static HoopoeClassLoader fromResource(String absoluteZipResourcePath,
                                                 ClassLoader parentClassLoader) {
        return fromStream(
                HoopoeClassLoader.class.getResourceAsStream(absoluteZipResourcePath),
                parentClassLoader);
    }

    private HoopoeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                try {
                    clazz = findClass(name);
                }
                catch (ClassNotFoundException e) {
                    clazz = getParent().loadClass(name);
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        ClassLoader parent = getParent();
        Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[parent == null ? 1 : 2];
        tmp[0] = findResources(name);
        if (parent != null) {
            tmp[1] = parent.getResources(name);
        }
        return new CompoundEnumeration<>(tmp);
    }

    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null && getParent() != null) {
            url = getParent().getResource(name);
        }
        return url;
    }

    private static void copyFile(ZipInputStream zipStream, File targetFile) throws IOException {
        targetFile.getParentFile().mkdirs();

        int bytesRead;
        byte[] buffer = new byte[8192 * 2];
        try (OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            while ((bytesRead = zipStream.read(buffer)) != -1) {
                fileStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private class CompoundEnumeration<E> implements Enumeration<E> {
        private Enumeration<E>[] enums;
        private int index = 0;

        public CompoundEnumeration(Enumeration<E>[] enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (this.index < this.enums.length) {
                if (this.enums[this.index] != null && this.enums[this.index].hasMoreElements()) {
                    return true;
                }

                ++this.index;
            }

            return false;
        }

        public boolean hasMoreElements() {
            return this.next();
        }

        public E nextElement() {
            if (!this.next()) {
                throw new NoSuchElementException();
            }
            else {
                return this.enums[this.index].nextElement();
            }
        }
    }

}