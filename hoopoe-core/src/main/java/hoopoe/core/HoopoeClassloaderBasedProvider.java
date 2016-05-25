package hoopoe.core;

import hoopoe.utils.HoopoeClassLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class HoopoeClassloaderBasedProvider {

    private static final String HOOPOE_CONFIG_FILE = "/META-INF/hoopoe.properties";

    protected Object load(String extensionlessZipResourceName) {
        try {
            String zipResourceName = extensionlessZipResourceName + ".zip";
            HoopoeClassLoader pluginClassLoader = getHoopoeClassLoader(zipResourceName);
            Properties properties = getProperties(zipResourceName, pluginClassLoader);
            Class clazz = getTargetClass(zipResourceName, pluginClassLoader, properties);
            return clazz.newInstance();
        }
        catch (IOException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private HoopoeClassLoader getHoopoeClassLoader(String zipResourceName) {
        ClassLoader parentClassloader = getClass().getClassLoader();

        InputStream pluginZipFileStream = parentClassloader.getResourceAsStream(zipResourceName);
        if (pluginZipFileStream == null) {
            throw new IllegalStateException(zipResourceName + " is not found.");
        }

        return HoopoeClassLoader.fromStream(pluginZipFileStream, parentClassloader);
    }

    private Properties getProperties(String zipResourceName, HoopoeClassLoader pluginClassLoader) throws IOException {
        InputStream propertiesStream = pluginClassLoader.getResourceAsStream(HOOPOE_CONFIG_FILE);
        if (propertiesStream == null) {
           throw new IllegalStateException(zipResourceName +
                   " does not contain " + HOOPOE_CONFIG_FILE + " file.");
        }

        Properties properties = new Properties();
        properties.load(propertiesStream);
        return properties;
    }

    private Class getTargetClass(String zipResourceName, HoopoeClassLoader pluginClassLoader, Properties properties) {
        String classNameProperty = getClassNameProperty();
        String className = properties.getProperty(classNameProperty);

        if (className == null) {
            throw new IllegalStateException(HOOPOE_CONFIG_FILE + " in " + zipResourceName +
                    " does not contain " + classNameProperty + " property.");
        }

        Class clazz;
        try {
            clazz = pluginClassLoader.loadClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(zipResourceName + " does not contain class " + className + ".");
        }
        return clazz;
    }

    protected abstract String getClassNameProperty();

}
