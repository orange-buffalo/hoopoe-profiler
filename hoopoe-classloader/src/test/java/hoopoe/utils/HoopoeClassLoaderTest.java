package hoopoe.utils;

import java.io.InputStream;
import java.lang.reflect.Method;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import overlapped.ClassZ;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class HoopoeClassLoaderTest {

    @Test(expected = ClassNotFoundException.class)
    public void testClassNotFound() throws ClassNotFoundException {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("simple-class-without-package.zip");
        hoopoeClassLoader.loadClass("ClassD");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testClassInRoot() throws ClassNotFoundException {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("class-in-root.zip");
        hoopoeClassLoader.loadClass("ClassA");
    }

    @Test
    public void testParentPropagation() throws ClassNotFoundException {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("simple-class-without-package.zip");
        Class stringClass = hoopoeClassLoader.loadClass("java.lang.String");

        assertThat(stringClass.getClassLoader(), not(equalTo(hoopoeClassLoader)));
    }

    @Test
    public void testSimpleClassWithoutPackage() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("simple-class-without-package.zip");
        Class<?> classA = hoopoeClassLoader.loadClass("ClassA");

        assertThat(classA.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classA.getMethod("getString");
        Object aInstance = classA.newInstance();
        assertThat(method.invoke(aInstance), equalTo("string"));
    }

    @Test
    public void testSimpleClassInPackage() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("simple-class-in-package.zip");
        Class<?> classB = hoopoeClassLoader.loadClass("nested.ClassB");

        assertThat(classB.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classB.getMethod("getInt");
        Object bInstance = classB.newInstance();
        assertThat(method.invoke(bInstance), equalTo(42));
    }

    @Test
    public void testSimpleClassInDeepPackage() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("simple-class-in-deep-package.zip");
        Class<?> classD = hoopoeClassLoader.loadClass("nested.deep.ClassD");

        assertThat(classD.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classD.getMethod("getChar");
        Object dInstance = classD.newInstance();
        assertThat(method.invoke(dInstance), equalTo('c'));
    }

    @Test
    public void testClassWithDependencies() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("class-with-dependencies.zip");
        Class<?> classC = hoopoeClassLoader.loadClass("nested.deep.ClassC");

        assertThat(classC.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classC.getMethod("getDelegatedInt");
        Object cInstance = classC.newInstance();
        assertThat(method.invoke(cInstance), equalTo(42));

        method = classC.getMethod("getB");
        Object bInstance = method.invoke(cInstance);
        assertThat(bInstance.getClass().getClassLoader(), equalTo(hoopoeClassLoader));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testJarInRoot() throws ClassNotFoundException {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("jar-in-root.zip");
        hoopoeClassLoader.loadClass("nested.ClassB");
    }

    @Test
    public void testJarInLib() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("jar-in-lib.zip");
        Class<?> classB = hoopoeClassLoader.loadClass("nested.ClassB");

        assertThat(classB.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classB.getMethod("getInt");
        Object bInstance = classB.newInstance();
        assertThat(method.invoke(bInstance), equalTo(42));
    }

    @Test
    public void testDependencyToJar() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("dependency-to-jar.zip");
        Class<?> classC = hoopoeClassLoader.loadClass("nested.deep.ClassC");

        assertThat(classC.getClassLoader(), equalTo(hoopoeClassLoader));

        Method method = classC.getMethod("getDelegatedInt");
        Object cInstance = classC.newInstance();
        assertThat(method.invoke(cInstance), equalTo(42));

        method = classC.getMethod("getB");
        Object bInstance = method.invoke(cInstance);
        assertThat(bInstance.getClass().getClassLoader(), equalTo(hoopoeClassLoader));
    }

    @Test
    public void testMetaInfResource() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("meta-inf.zip");
        InputStream stream = hoopoeClassLoader.getResourceAsStream("META-INF/data.txt");
        assertThat(stream, notNullValue());

        String data = IOUtils.toString(stream, "utf-8");
        assertThat(data, equalTo("data"));
    }

    @Test
    public void testResourceInClassesRoot() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("resource-in-classes-root.zip");
        InputStream stream = hoopoeClassLoader.getResourceAsStream("data.txt");
        assertThat(stream, notNullValue());

        String data = IOUtils.toString(stream, "utf-8");
        assertThat(data, equalTo("data"));
    }

    @Test
    public void testHierarchyCorrectness() throws Exception {
        ClassLoader hoopoeClassLoader = getHoopoeClassLoader("overlapped-class.zip");
        Class<?> zippedClassZ = hoopoeClassLoader.loadClass("overlapped.ClassZ");

        assertThat(zippedClassZ.getClassLoader(), equalTo(hoopoeClassLoader));
        assertThat(zippedClassZ, not(CoreMatchers.equalTo(ClassZ.class)));
    }

    private HoopoeClassLoader getHoopoeClassLoader(String zipName) {
        return HoopoeClassLoader.fromResource("/classloader/" + zipName, getClass().getClassLoader());
    }

}