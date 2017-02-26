package hoopoe.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(DataProviderRunner.class)
public class MetadataReaderTest {

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
        return new Object[][]{
                {new TypeDescription.ForLoadedType(MetadataReader.class),
                        "hoopoe.core.MetadataReader"},

                {new TypeDescription.ForLoadedType(NestedClass.class),
                        "hoopoe.core.MetadataReaderTest.NestedClass"},

                {new TypeDescription.ForLoadedType(ArrayList.class),
                        "java.util.ArrayList"},

                {new TypeDescription.Generic.OfParameterizedType.ForLoadedType(ABSTRACT_LIST),
                        "java.util.AbstractList"},

                {new TypeDescription.ForLoadedType(SUPPLIER_LAMBDA.getClass()), "" +
                        "hoopoe.core.MetadataReaderTest$$Lambda$1"},

                {new TypeDescription.ForLoadedType(SUPPLIER_ANONYMOUS_IMPL.getClass()),
                        "hoopoe.core.MetadataReaderTest$1"}
        };
    }

    @UseDataProvider
    @Test
    public void testGetClassName(TypeDefinition inputType, String expectedName) {
        MetadataReader metadataReader = new MetadataReader();
        String actualName = metadataReader.getClassName(inputType);
        assertThat(actualName, equalTo(expectedName));
    }

    public static class NestedClass {

    }

}