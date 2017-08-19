package hoopoe.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import static org.hamcrest.Matchers.equalTo;

@RunWith(DataProviderRunner.class)
public class ClassMetadataReaderTest {

    private static final Supplier SUPPLIER_LAMBDA = () -> null;

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
                        "hoopoe.core.ClassMetadataReader"},

                {new TypeDescription.ForLoadedType(NestedClass.class),
                        "hoopoe.core.ClassMetadataReaderTest.NestedClass"},

                {new TypeDescription.ForLoadedType(ArrayList.class),
                        "java.util.ArrayList"},

                {new TypeDescription.Generic.OfParameterizedType.ForLoadedType(ABSTRACT_LIST),
                        "java.util.AbstractList"},

                {new TypeDescription.ForLoadedType(SUPPLIER_LAMBDA.getClass()), "" +
                        "hoopoe.core.ClassMetadataReaderTest$$Lambda$1"},

                {new TypeDescription.ForLoadedType(SUPPLIER_ANONYMOUS_IMPL.getClass()),
                        "hoopoe.core.ClassMetadataReaderTest$1"}
        };
    }

    @UseDataProvider
    @Test
    public void testGetClassName(TypeDefinition inputType, String expectedName) {
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        String actualName = classMetadataReader.getClassName(inputType);
        assertThat(actualName, equalTo(expectedName));
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
                        "MethodsHolder()"
                },

                new Object[] {
                        new MethodDescription.ForLoadedConstructor(
                                MethodsHolder.class.getConstructor(String.class)),
                        "MethodsHolder(java.lang.String)"
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
                        "nestedClassParameter(hoopoe.core.ClassMetadataReaderTest.NestedClass)"
                },

                new Object[] {
                        new MethodDescription.ForLoadedMethod(
                                getMethodForGetMethodSignatureTest("multipleParametersMethod")),
                        "multipleParametersMethod(java.lang.Double[],java.lang.String,int)"
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

    public static class NestedClass {

    }

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