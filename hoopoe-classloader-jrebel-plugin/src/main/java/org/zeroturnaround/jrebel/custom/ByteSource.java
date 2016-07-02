package org.zeroturnaround.jrebel.custom;

import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.Resource;
import org.zeroturnaround.javarebel.integration.support.BaseClassResourceSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class ByteSource extends BaseClassResourceSource {

    private WeakReference<ClassLoader> classLoader;

    public ByteSource(ClassLoader classLoader) {
        this.classLoader = new WeakReference<ClassLoader>(classLoader);
    }

    @Override
    public Resource[] getLocalResources(String name) {
        return new Resource[]{getLocalResource(name)};
    }

    @Override
    public Resource getLocalResource(String name) {
        InputStream resourceAsStream = classLoader.get().getResourceAsStream(name);
        if (resourceAsStream != null){
            try {
                return new ByteResource(name, streamToByteArray(resourceAsStream));
            } catch (IOException exception) {
                LoggerFactory.getLogger("Custom-Hoopoe").error(exception);
            }
        }

        return null;
    }

    private byte[] streamToByteArray(InputStream resourceAsStream) throws IOException {
        int len;
        int size = 1024;
        byte[] buffer;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        buffer = new byte[size];
        while ((len = resourceAsStream.read(buffer, 0, size)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        buffer = byteArrayOutputStream.toByteArray();

        return buffer;
    }
}
