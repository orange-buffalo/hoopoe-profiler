package hoopoe.core.components;

import hoopoe.api.plugins.HoopoePlugin;
import hoopoe.core.HoopoeException;
import hoopoe.utils.HoopoeClassLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Loads plugin or extension by provided path, wrapping it into {@link HoopoeClassLoader}.
 */
public class ComponentLoader {

    private static final String HOOPOE_CONFIG_FILE = "META-INF/hoopoe.properties";
    private static final String PLUGIN_CLASS_PROPERTY = "plugin.className";
    private static final String EXTENSION_CLASS_PROPERTY = "extension.className";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private ClassLoader parentClassLoader;

    public ComponentLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    /**
     * Loads plugin or extension by provided path. If path starts with "classpath:", tries to load zip file from the
     * parent classloader. If path starts with "file:", tries to load zip from file by absolute path. if no type
     * specified, tries to load from file. Creates and instance of {@link HoopoeClassLoader} for the component zip.
     * Loads the component class from created classloader, verifies if it is assignable to provided class, and
     * instantiates the component using default constructor.
     *
     * @param componentZipPath path to load component by.
     * @param componentClass   {@link HoopoePlugin} or {@link hoopoe.api.extensions.HoopoeProfilerExtension} expected.
     *
     * @return instantiated plugin or extension.
     */
    public <T> T loadComponent(
            String componentZipPath,
            Class<T> componentClass) {

        try {
            URL zipUrl = getZipUrl(componentZipPath);
            HoopoeClassLoader pluginClassLoader = getHoopoeClassLoader(zipUrl);
            Properties properties = getComponentProperties(componentZipPath, pluginClassLoader);
            Class<T> componentClassInZip =
                    getComponentClass(componentZipPath, pluginClassLoader, properties, componentClass);
            return componentClassInZip.newInstance();
        } catch (IOException | InstantiationException | IllegalAccessException e) {
            throw new HoopoeException("Cannot load " + componentZipPath, e);
        }
    }

    private URL getZipUrl(String zipPath) throws MalformedURLException {
        if (zipPath.startsWith(CLASSPATH_PREFIX)) {
            String resourceName = zipPath.replace(CLASSPATH_PREFIX, "");
            URL zipUrl = parentClassLoader.getResource(resourceName);
            if (zipUrl == null) {
                throw new HoopoeException("Cannot find resource " + resourceName + " in " + parentClassLoader);
            }
            return zipUrl;
        } else {
            String zipFilePath = zipPath.replace(FILE_PREFIX, "");
            File zipFile = new File(zipFilePath);
            if (!zipFile.exists()) {
                throw new HoopoeException("Cannot find file " + zipFilePath);
            }
            return zipFile.toURI().toURL();
        }
    }

    private HoopoeClassLoader getHoopoeClassLoader(URL zipUrl) throws IOException {
        try (InputStream zipStream = zipUrl.openStream()) {
            return HoopoeClassLoader.fromStream(zipStream, parentClassLoader);
        }
    }

    private Properties getComponentProperties(
            String componentZipPath,
            HoopoeClassLoader componentClassLoader) throws IOException {

        try (InputStream propertiesStream = componentClassLoader.getResourceAsStream(HOOPOE_CONFIG_FILE)) {
            if (propertiesStream == null) {
                throw new HoopoeException(componentZipPath + " does not contain " + HOOPOE_CONFIG_FILE + " file");
            }

            Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        }
    }

    private <T> Class<T> getComponentClass(
            String componentZipPath,
            HoopoeClassLoader pluginClassLoader,
            Properties properties,
            Class<T> expectedComponentClass) {

        String classNameProperty = HoopoePlugin.class.isAssignableFrom(expectedComponentClass)
                ? PLUGIN_CLASS_PROPERTY : EXTENSION_CLASS_PROPERTY;
        String className = properties.getProperty(classNameProperty);

        if (className == null) {
            throw new HoopoeException(HOOPOE_CONFIG_FILE + " in " + componentZipPath +
                    " does not contain " + classNameProperty + " property");
        }

        Class actualComponentClazz;
        try {
            actualComponentClazz = pluginClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new HoopoeException(componentZipPath + " does not contain class " + className);
        }

        if (!expectedComponentClass.isAssignableFrom(actualComponentClazz)) {
            throw new HoopoeException("Error while loading " + componentZipPath + ": " +
                    actualComponentClazz.getCanonicalName() + " cannot be assigned to " +
                    expectedComponentClass.getCanonicalName());
        }

        return actualComponentClazz;
    }
}
