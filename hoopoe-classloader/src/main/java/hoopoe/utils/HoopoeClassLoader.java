package hoopoe.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class HoopoeClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    private Map<String, byte[]> zipData = new HashMap<>();

    public static HoopoeClassLoader fromStream(InputStream stream,
                                               ClassLoader parentClassLoader) {
        return new HoopoeClassLoader(stream, parentClassLoader);
    }

    public static HoopoeClassLoader fromResource(String absoluteZipResourcePath,
                                                 ClassLoader parentClassLoader) {
        return new HoopoeClassLoader(
                HoopoeClassLoader.class.getResourceAsStream(absoluteZipResourcePath),
                parentClassLoader);
    }

    private HoopoeClassLoader(InputStream inputStream,
                              ClassLoader parentClassLoader) {
        super(parentClassLoader);

        try (ZipInputStream stream = new ZipInputStream(inputStream)) {
            initClassData(stream);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {

                String zipDataName = "classes/" + name.replaceAll("\\.", "/") + ".class";
                byte[] classBytes = zipData.get(zipDataName);
                if (classBytes != null) {
                    //zipData.remove(zipDataName);
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

    @Override
    public InputStream getResourceAsStream(String name) {
        String zippedName = name;
        if (zippedName.startsWith("/")) {
            zippedName = zippedName.substring(1);
        }
        if (!zippedName.startsWith("META-INF")) {
            zippedName = "classes/" + zippedName;
        }
        byte[] bytes = zipData.get(zippedName);
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }
        else if (getParent() != null) {
            return getParent().getResourceAsStream(name);
        }
        else {
            return null;
        }
    }

    private void initClassData(ZipInputStream stream) throws IOException {
        ZipEntry nextEntry;
        while ((nextEntry = stream.getNextEntry()) != null) {
            if (!nextEntry.isDirectory()) {
                String name = nextEntry.getName();
                if (name.startsWith("classes/") || name.startsWith("META-INF/")) {
                    byte[] entryData = getZipEntryBytes(stream, nextEntry);
                    zipData.put(name, entryData);
                }
                else if (name.startsWith("lib/") && name.endsWith(".jar")) {
                    byte[] jarBytes = getZipEntryBytes(stream, nextEntry);
                    loadJarClasses(new ByteArrayInputStream(jarBytes));
                }
            }
        }
    }

    private void loadJarClasses(InputStream jarStream) throws IOException {
        ZipEntry jarEntry;
        try (ZipInputStream zipInputStream = new ZipInputStream(jarStream)) {
            while ((jarEntry = zipInputStream.getNextEntry()) != null) {
                if (!jarEntry.isDirectory()) {
                    byte[] entryBytes = getZipEntryBytes(zipInputStream, jarEntry);
                    zipData.put("classes/" + jarEntry.getName(), entryBytes);
                }
            }
        }
    }

    private byte[] getZipEntryBytes(ZipInputStream zipStream, ZipEntry streamEntry) throws IOException {
        ByteArrayOutputStream entryStream = new ByteArrayOutputStream();
        int bytesRead;
        byte[] buffer = new byte[8192 * 2];
        while ((bytesRead = zipStream.read(buffer)) != -1) {
            entryStream.write(buffer, 0, bytesRead);
        }
        return entryStream.toByteArray();
    }

}