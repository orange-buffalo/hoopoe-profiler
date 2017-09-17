package hoopoe.core.instrumentation;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.plugins.HoopoeMethodInfo;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(DataProviderRunner.class)
public class ClassMetadataReaderTest {

    private static final Supplier SUPPLIER_LAMBDA = () -> null;

    @SuppressWarnings("Convert2Lambda")
    private static final Supplier SUPPLIER_ANONYMOUS_IMPL = new Supplier() {
        @Override
        public Object get() {
            return null;
        }
    };

    private static final ParameterizedType ABSTRACT_LIST = (ParameterizedType) ArrayList.class.getGenericSuperclass();

    @DataProvider
    public static Object[][] testGetClassName() {
        return new Object[][] {
                {new TypeDescription.ForLoadedType(ClassMetadataReader.class),
                        "hoopoe.core.instrumentation.ClassMetadataReader"},

                {new TypeDescription.ForLoadedType(NestedClass.class),
                        "hoopoe.core.instrumentation.ClassMetadataReaderTest.NestedClass"},

                {new TypeDescription.ForLoadedType(ArrayList.class),
                        "java.util.ArrayList"},

                {new TypeDescription.Generic.OfParameterizedType.ForLoadedType(ABSTRACT_LIST),
                        "java.util.AbstractList"},

                {new TypeDescription.ForLoadedType(SUPPLIER_LAMBDA.getClass()), "" +
                        "hoopoe.core.instrumentation.ClassMetadataReaderTest$$Lambda$"},

                {new TypeDescription.ForLoadedType(SUPPLIER_ANONYMOUS_IMPL.getClass()),
                        "hoopoe.core.instrumentation.ClassMetadataReaderTest$"}
        };
    }

    @UseDataProvider
    @Test
    public void testGetClassName(TypeDefinition inputType, String expectedName) {
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        String actualName = classMetadataReader.getClassName(inputType);
        assertThat(actualName, containsString(expectedName));
    }

    @DataProvider
    public static Object[][] testGetSuperclassesNames() {
        return new Object[][] {
                {new TypeDescription.ForLoadedType(Object.class),
                        Collections.emptyList()},

                {new TypeDescription.ForLoadedType(NestedClass.class),
                        Collections.singleton("java.lang.Object")},

                {new TypeDescription.ForLoadedType(ArrayList.class),
                        Arrays.asList(
                                "java.util.List", "java.util.RandomAccess", "java.lang.Cloneable",
                                "java.io.Serializable", "java.util.AbstractList", "java.util.AbstractCollection",
                                "java.util.Collection", "java.lang.Object", "java.lang.Iterable"
                        )
                },

                {new TypeDescription.ForLoadedType(MethodInfoClass.class),
                        Arrays.asList("java.lang.Object", "java.io.Serializable")
                }
        };
    }

    @UseDataProvider
    @Test
    public void testGetSuperclassesNames(TypeDefinition inputType, Collection<String> expectedSuperClasses) {
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        Collection<String> actualSuperClasses = classMetadataReader.getSuperClassesNames(inputType);
        assertThat(actualSuperClasses,
                containsInAnyOrder(expectedSuperClasses.toArray(new String[expectedSuperClasses.size()])));
    }

    @DataProvider
    public static Object[][] testGetMethodSignature() throws NoSuchMethodException {
        return new Object[][] {
                new Object[] {
                        new MethodDescription.ForLoadedConstructor(
                                MethodsHolder.class.getConstructor()),
                        "<init>()"
                },

                new Object[] {
                        new MethodDescription.ForLoadedConstructor(
                                MethodsHolder.class.getConstructor(String.class)),
                        "<init>(java.lang.String)"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("singleIntMethod")),
                        "singleIntMethod(int)"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("singleIntegerMethod")),
                        "singleIntegerMethod(java.lang.Integer)"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("primitiveArrayMethod")),
                        "primitiveArrayMethod(float[])"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("objectArrayMethod")),
                        "objectArrayMethod(java.lang.Double[])"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("nestedClassParameter")),
                        "nestedClassParameter(hoopoe.core.instrumentation.ClassMetadataReaderTest.NestedClass)"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("multipleParametersMethod")),
                        "multipleParametersMethod(java.lang.Double[],java.lang.String,int)"
                },

                new Object[] {
                        new MethodDescription.Latent.TypeInitializer(
                                new TypeDescription.ForLoadedType(NestedClass.class)),
                        "<clinit>()"
                }
        };
    }

    private static Method getMethodForGetMethodSignatureTest(String name) {
        for (Method method : MethodsHolder.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException(name + " not found");
    }

    @UseDataProvider
    @Test
    public void testGetMethodSignature(MethodDescription methodDescription, String expectedSignature) {
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        String actualMethodSignature = classMetadataReader.getMethodSignature(methodDescription);
        assertThat(actualMethodSignature, equalTo(expectedSignature));
    }

    @Test
    public void testCreateMethodInfo() throws NoSuchMethodException {
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        HoopoeMethodInfo methodInfo = classMetadataReader.createMethodInfo(
                new MethodDescription.ForLoadedMethod(MethodInfoClass.class.getMethod("doMethod", String.class))
        );

        assertThat("Method info should be created", methodInfo, notNullValue());

        assertThat("Class name should be calculated",
                methodInfo.getCanonicalClassName(),
                equalTo("hoopoe.core.instrumentation.ClassMetadataReaderTest.MethodInfoClass"));

        assertThat("Method signature should be calculated",
                methodInfo.getMethodSignature(),
                equalTo("doMethod(java.lang.String)"));

        assertThat("Owner class superclasses should be calculated",
                methodInfo.getSuperclasses(),
                containsInAnyOrder("java.lang.Object", "java.io.Serializable"));
    }

    @SuppressWarnings("unused")
    public static class MethodInfoClass implements Serializable {

        public int doMethod(String param) {
            return 42;
        }
    }

    public static class NestedClass {

        static {
            // describe me!
        }

    }

    @SuppressWarnings("unused")
    public static class MethodsHolder {

        public MethodsHolder() {
        }

        public MethodsHolder(String constructorParam) {
        }

        public void singleIntMethod(int param) {

        }

        public void singleIntegerMethod(Integer param) {

        }

        public void primitiveArrayMethod(float[] param) {

        }

        public void objectArrayMethod(Double[] param) {

        }

        public void nestedClassParameter(NestedClass nestedClass) {

        }

        public void multipleParametersMethod(Double[] param, String anotherParam, int value) {

        }

    }

}