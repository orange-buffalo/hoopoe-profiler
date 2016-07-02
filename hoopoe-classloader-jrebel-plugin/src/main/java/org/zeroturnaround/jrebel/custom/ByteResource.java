package org.zeroturnaround.jrebel.custom;

import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;


public class ByteResource implements Resource {

    private byte[] bytes;
    private final String spec;

    public ByteResource(String path, byte[] bytes){
        spec = "jrbytecode:" + path;
        this.bytes = bytes;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public long lastModified() {
        return 1;
    }

    public URL toURL() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                public URL run() throws MalformedURLException {
                    return new URL(null, spec, new BytecodeURLStreamHandler());
                }
            });
        } catch (PrivilegedActionException exception) {
            LoggerFactory.getLogger("Custom-Hoopoe").error(exception);
            return null;
        }
    }

    private class BytecodeURLStreamHandler extends URLStreamHandler {
        protected URLConnection openConnection(final URL u) {
            return new BytecodeURLConnection(u);
        }
    }

    private class BytecodeURLConnection extends URLConnection {
        protected BytecodeURLConnection(URL url) {
            super(url);
        }
        public void connect() throws IOException {
        }
        public InputStream getInputStream() {
            return new ByteArrayInputStream(getBytes());
        }
        public int getContentLength() {
            return getBytes().length;
        }
    }
}
