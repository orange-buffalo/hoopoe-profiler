package hoopoe.core.components;

import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.api.plugins.HoopoePlugin;
import hoopoe.utils.HoopoeClassLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

public class ComponentLoaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ClassLoader parentClassLoader = new ClassLoader(ComponentLoaderTest.class.getClassLoader()) {
    };

    private ComponentLoader componentLoader = new ComponentLoader(parentClassLoader);

    @Test
    public void testLoadPluginFromClasspath() {
        Object hoopoePlugin =
                componentLoader.loadComponent("classpath:hoopoe/core/components/plugin.zip", HoopoePlugin.class);

        assertThat(hoopoePlugin, notNullValue());
        assertThat(hoopoePlugin, instanceOf(HoopoePlugin.class));

        Class pluginClass = hoopoePlugin.getClass();
        assertThat(pluginClass.getCanonicalName(), equalTo("hoopoe.core.components.TestPlugin"));

        ClassLoader pluginClassLoader = pluginClass.getClassLoader();
        assertThat(pluginClassLoader, instanceOf(HoopoeClassLoader.class));
        assertThat(pluginClassLoader.getParent(), equalTo(parentClassLoader));
    }

    @Test
    public void testLoadPluginFromFile() throws IOException {
        File tmpFile = temporaryFolder.newFile();
        IOUtils.copy(
                getClass().getResourceAsStream("/hoopoe/core/components/plugin.zip"),
                Files.newOutputStream(tmpFile.toPath())
        );

        Object hoopoePlugin =
                componentLoader.loadComponent("file:" + tmpFile.getAbsolutePath(), HoopoePlugin.class);

        assertThat(hoopoePlugin, notNullValue());
        assertThat(hoopoePlugin, instanceOf(HoopoePlugin.class));

        Class pluginClass = hoopoePlugin.getClass();
        assertThat(pluginClass.getCanonicalName(), equalTo("hoopoe.core.components.TestPlugin"));

        ClassLoader pluginClassLoader = pluginClass.getClassLoader();
        assertThat(pluginClassLoader, instanceOf(HoopoeClassLoader.class));
        assertThat(pluginClassLoader.getParent(), equalTo(parentClassLoader));
    }

    @Test
    public void testLoadPluginFromUnspecifiedSource() throws IOException {
        File tmpFile = temporaryFolder.newFile();
        IOUtils.copy(
                getClass().getResourceAsStream("/hoopoe/core/components/plugin.zip"),
                Files.newOutputStream(tmpFile.toPath())
        );

        Object hoopoePlugin =
                componentLoader.loadComponent(tmpFile.getAbsolutePath(), HoopoePlugin.class);

        assertThat(hoopoePlugin, notNullValue());
        assertThat(hoopoePlugin, instanceOf(HoopoePlugin.class));

        Class pluginClass = hoopoePlugin.getClass();
        assertThat(pluginClass.getCanonicalName(), equalTo("hoopoe.core.components.TestPlugin"));

        ClassLoader pluginClassLoader = pluginClass.getClassLoader();
        assertThat(pluginClassLoader, instanceOf(HoopoeClassLoader.class));
        assertThat(pluginClassLoader.getParent(), equalTo(parentClassLoader));
    }

    @Test
    public void testExceptionWhenZipIsNotFoundOnClassPath() {
        expectedException.expectMessage("Cannot find resource not-existing-path in " + parentClassLoader);

        componentLoader.loadComponent("classpath:not-existing-path", HoopoePlugin.class);
    }

    @Test
    public void testExceptionWhenZipIsNotFoundOnFileSystem() {
        expectedException.expectMessage("Cannot find file not-existing-path");

        componentLoader.loadComponent("file:not-existing-path", HoopoePlugin.class);
    }

    @Test
    public void testExceptionWhenPropertiesFileInMissing() {
        expectedException.expectMessage(
                "classpath:hoopoe/core/components/without-properties.zip does " +
                        "not contain META-INF/hoopoe.properties file");

        componentLoader.loadComponent("classpath:hoopoe/core/components/without-properties.zip", HoopoePlugin.class);
    }

    @Test
    public void testExceptionWhenClassNameIsMissingInProperties() {
        expectedException.expectMessage(
                "META-INF/hoopoe.properties in classpath:hoopoe/core/components/properties-without-class-name.zip " +
                        "does not contain plugin.className property");

        componentLoader.loadComponent(
                "classpath:hoopoe/core/components/properties-without-class-name.zip", HoopoePlugin.class);
    }

    @Test
    public void testExceptionWhenClassIsMissing() {
        expectedException.expectMessage(
                "classpath:hoopoe/core/components/without-class.zip does " +
                        "not contain class hoopoe.core.components.TestPlugin");

        componentLoader.loadComponent(
                "classpath:hoopoe/core/components/without-class.zip", HoopoePlugin.class);
    }

    @Test
    public void testExceptionWhenClassIsNotImplementingProperInterface() {
        expectedException.expectMessage(
                "Error while loading classpath:hoopoe/core/components/not-a-plugin.zip: " +
                        "hoopoe.core.components.TestPlugin cannot be assigned to hoopoe.api.plugins.HoopoePlugin");

        componentLoader.loadComponent(
                "classpath:hoopoe/core/components/not-a-plugin.zip", HoopoePlugin.class);
    }

    @Test
    public void testInstantiationExceptionWhenCreatingComponent() {
        expectedException.expectMessage(
                "Cannot load classpath:hoopoe/core/components/plugin-with-private-constructor.zip");

        componentLoader.loadComponent(
                "classpath:hoopoe/core/components/plugin-with-private-constructor.zip", HoopoePlugin.class);
    }

    @Test
    public void testLoadExtensionFromClasspath() {
        Object hoopoeExtension = componentLoader.loadComponent(
                "classpath:hoopoe/core/components/extension.zip",
                HoopoeProfilerExtension.class);

        assertThat(hoopoeExtension, notNullValue());
        assertThat(hoopoeExtension, instanceOf(HoopoeProfilerExtension.class));

        Class pluginClass = hoopoeExtension.getClass();
        assertThat(pluginClass.getCanonicalName(), equalTo("hoopoe.core.components.TestExtension"));

        ClassLoader pluginClassLoader = pluginClass.getClassLoader();
        assertThat(pluginClassLoader, instanceOf(HoopoeClassLoader.class));
        assertThat(pluginClassLoader.getParent(), equalTo(parentClassLoader));
    }
}