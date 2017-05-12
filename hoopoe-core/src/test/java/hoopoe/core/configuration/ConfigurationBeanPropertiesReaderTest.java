package hoopoe.core.configuration;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.configuration.HoopoeConfigurationProperty;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(DataProviderRunner.class)
public class ConfigurationBeanPropertiesReaderTest {

    @Rule
    public ExpectedException thrownException = ExpectedException.none();

    @DataProvider
    public static Object[][] testGetProperties() {
        return new Object[][] {
                {new TestDataItem(ClassWithoutProperties.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithGetterOnly.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithAnnotatedGetterWithoutSetter.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithSetterOnly.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithAnnotatedSetterWithoutGetter.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithAnnotatedGetterAndSetter.class)
                        .expectException(ClassWithAnnotatedGetterAndSetter.class.getCanonicalName() +
                        " has annotated getter and setter for property 'value'. This is not supported.")},

                {new TestDataItem(ClassWithGetterAndSetterWithoutAnnotation.class)
                        .expectNoProperties()},

                {new TestDataItem(ClassWithDefaultAnnotationAttributesOnSetter.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithDefaultAnnotationAttributesOnSetter.class, "value").build())},

                {new TestDataItem(ClassWithDefaultAnnotationAttributesOnGetter.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithDefaultAnnotationAttributesOnGetter.class, "value").build())},

                {new TestDataItem(ClassWithCustomAnnotationValues.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithCustomAnnotationValues.class, "value")
                                .key("propertyKey")
                                .name("propertyName")
                                .description("propertyDescription")
                                .build())},

                {new TestDataItem(ClassWithCustomPropertyName.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithCustomPropertyName.class, "value")
                                .name("propertyName")
                                .build())},

                {new TestDataItem(ClassWithCustomPropertyKey.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithCustomPropertyKey.class, "value")
                                .key("propertyKey")
                                .name("Property Key")
                                .build())},

                {new TestDataItem(ClassWithCustomPropertyKeyAndName.class)
                        .expectProperty(
                        prepareDefaultProperty(ClassWithCustomPropertyKeyAndName.class, "value")
                                .key("propertyKey")
                                .name("propertyName")
                                .build())},

                {new TestDataItem(ClassWithMultiplesAnnotatedProperties.class)
                        .expectProperty(
                                prepareDefaultProperty(ClassWithMultiplesAnnotatedProperties.class, "value").build())
                        .expectProperty(
                                prepareDefaultProperty(ClassWithMultiplesAnnotatedProperties.class, "answer")
                                        .build())},

                {new TestDataItem(ChildClassWithAnnotatedProperty.class)
                        .expectProperty(
                                prepareDefaultProperty(ChildClassWithAnnotatedProperty.class, "value").build())
                        .expectProperty(
                                prepareDefaultProperty(ChildClassWithAnnotatedProperty.class, "answer")
                                        .build())},

                {new TestDataItem(ClassWithAllSupportedPropertyTypes.class)
                        .expectProperty(
                                prepareDefaultProperty(ClassWithAllSupportedPropertyTypes.class, "stringValue")
                                        .name("String Value")
                                        .build())
                        .expectProperty(
                                prepareDefaultProperty(ClassWithAllSupportedPropertyTypes.class, "intValue")
                                        .name("Int Value")
                                        .build())
                        .expectProperty(
                                prepareDefaultProperty(ClassWithAllSupportedPropertyTypes.class, "integerValue")
                                        .name("Integer Value")
                                        .build())
                        .expectProperty(
                                prepareDefaultProperty(ClassWithAllSupportedPropertyTypes.class, "objectLongValue")
                                        .name("Object Long Value")
                                        .build())
                        .expectProperty(
                                prepareDefaultProperty(ClassWithAllSupportedPropertyTypes.class, "primitiveLongValue")
                                        .name("Primitive Long Value")
                                        .build())},

                {new TestDataItem(ClassWithUnsupportedPropertyType.class)
                        .expectException(ClassWithUnsupportedPropertyType.class.getCanonicalName() +
                        " has annotated property 'value' for unsupported property type java.lang.Object")},

                {new TestDataItem(ClassWithPropertyWithIndexedAccessOnly.class)
                        // no properties expected as property is not read-write
                        .expectNoProperties()}
        };
    }

    private static ConfigurationBeanProperty.ConfigurationBeanPropertyBuilder prepareDefaultProperty(
            Class configurationBeanClass, String propertyName) {

        String nameWithFirstLetterCapitalized = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        return ConfigurationBeanProperty.builder()
                .key(propertyName)
                .name(nameWithFirstLetterCapitalized)
                .description("")
                .getter(getMethod(configurationBeanClass, "get" + nameWithFirstLetterCapitalized))
                .setter(getMethod(configurationBeanClass, "set" + nameWithFirstLetterCapitalized));
    }

    private static Method getMethod(Class clazz, String name) {
        return Stream.of(clazz.getMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    @UseDataProvider
    @Test
    public void testGetProperties(TestDataItem testDataItem) {
        if (testDataItem.expectedExceptionMessage != null) {
            thrownException.expectMessage(equalTo(testDataItem.expectedExceptionMessage));
        }

        ConfigurationBeanPropertiesReader reader =
                new ConfigurationBeanPropertiesReader(testDataItem.configurationBeanClass);

        Collection<ConfigurationBeanProperty> actualProperties = reader.getProperties();

        assertThat("Exception is expected but not thrown", testDataItem.expectedExceptionMessage, nullValue());

        assertThat("Calculated properties should never be null",
                actualProperties, notNullValue());
        assertThat("Calculated properties size is different from expected properties size",
                actualProperties.size(), equalTo(testDataItem.expectedProperties.size()));

        Map<String, ConfigurationBeanProperty> actualPropertiesByKey = actualProperties.stream()
                .collect(Collectors.toMap(ConfigurationBeanProperty::getKey, Function.identity()));

        Map<String, ConfigurationBeanProperty> expectedPropertiesByKey = testDataItem.expectedProperties.stream()
                .collect(Collectors.toMap(ConfigurationBeanProperty::getKey, Function.identity()));

        expectedPropertiesByKey.forEach((expectedPropertyKey, expectedProperty) -> {
            ConfigurationBeanProperty actualProperty = actualPropertiesByKey.get(expectedPropertyKey);

            assertThat("Property " + expectedPropertyKey + " is expected but not found",
                    actualProperty, notNullValue());

            assertThat("Property " + expectedPropertyKey + " has unexpected name",
                    actualProperty.getName(), equalTo(expectedProperty.getName()));

            assertThat("Property " + expectedPropertyKey + " has unexpected description",
                    actualProperty.getDescription(), equalTo(expectedProperty.getDescription()));

            assertThat("Property " + expectedPropertyKey + " has unexpected getter",
                    actualProperty.getGetter(), equalTo(expectedProperty.getGetter()));

            assertThat("Property " + expectedPropertyKey + " has unexpected setter",
                    actualProperty.getSetter(), equalTo(expectedProperty.getSetter()));

            assertThat("Property " + expectedPropertyKey + " has no converter",
                    actualProperty.getConverter(), notNullValue());
        });
    }

    public static class TestDataItem {
        private Class configurationBeanClass;
        private String expectedExceptionMessage;
        private List<ConfigurationBeanProperty> expectedProperties = new ArrayList<>();

        public TestDataItem(Class configurationBeanClass) {
            this.configurationBeanClass = configurationBeanClass;
        }

        public TestDataItem expectException(String errorMessage) {
            this.expectedExceptionMessage = errorMessage;
            return this;
        }

        public TestDataItem expectProperty(ConfigurationBeanProperty property) {
            this.expectedProperties.add(property);
            return this;
        }

        public TestDataItem expectNoProperties() {
            return this;
        }

        @Override
        public String toString() {
            return configurationBeanClass.getSimpleName();
        }
    }

    public static class ClassWithoutProperties {
        public String testSomething() {
            return "42";
        }
    }

    public static class ClassWithGetterOnly {
        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithAnnotatedGetterWithoutSetter {
        @HoopoeConfigurationProperty
        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithSetterOnly {
        public void setValue(String value) {
        }
    }

    public static class ClassWithAnnotatedSetterWithoutGetter {
        @HoopoeConfigurationProperty
        public void setValue(String value) {
        }
    }

    public static class ClassWithAnnotatedGetterAndSetter {
        @HoopoeConfigurationProperty
        public void setValue(String value) {
        }

        @HoopoeConfigurationProperty
        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithGetterAndSetterWithoutAnnotation {
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithDefaultAnnotationAttributesOnSetter {
        @HoopoeConfigurationProperty
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithDefaultAnnotationAttributesOnGetter {
        public void setValue(String value) {
        }

        @HoopoeConfigurationProperty
        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithCustomAnnotationValues {
        @HoopoeConfigurationProperty(name = "propertyName", description = "propertyDescription", key = "propertyKey")
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithCustomPropertyName {
        @HoopoeConfigurationProperty(name = "propertyName")
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithCustomPropertyKey {
        @HoopoeConfigurationProperty(key = "propertyKey")
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithCustomPropertyKeyAndName {
        @HoopoeConfigurationProperty(name = "propertyName", key = "propertyKey")
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ClassWithMultiplesAnnotatedProperties {
        @HoopoeConfigurationProperty
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }

        @HoopoeConfigurationProperty
        public void setAnswer(int answer) {
        }

        public int getAnswer() {
            return 42;
        }
    }

    public static class ParentClassWithAnnotatedProperty {
        @HoopoeConfigurationProperty
        public void setValue(String value) {
        }

        public String getValue() {
            return "42";
        }
    }

    public static class ChildClassWithAnnotatedProperty extends ParentClassWithAnnotatedProperty {
        @HoopoeConfigurationProperty
        public void setAnswer(int answer) {
        }

        public int getAnswer() {
            return 42;
        }
    }

    public static class ClassWithAllSupportedPropertyTypes {
        @HoopoeConfigurationProperty
        public void setStringValue(String value) {
        }

        public String getStringValue() {
            return "42";
        }

        @HoopoeConfigurationProperty
        public void setIntValue(int value) {
        }

        public int getIntValue() {
            return 42;
        }

        @HoopoeConfigurationProperty
        public void setIntegerValue(Integer value) {
        }

        public Integer getIntegerValue() {
            return 42;
        }

        @HoopoeConfigurationProperty
        public void setPrimitiveLongValue(long value) {
        }

        public long getPrimitiveLongValue() {
            return 42;
        }

        @HoopoeConfigurationProperty
        public void setObjectLongValue(Long value) {
        }

        public Long getObjectLongValue() {
            return 42L;
        }
    }

    public static class ClassWithUnsupportedPropertyType {
        @HoopoeConfigurationProperty
        public void setValue(Object value) {
        }

        public Object getValue() {
            return null;
        }

    }

    public static class ClassWithPropertyWithIndexedAccessOnly {
        @HoopoeConfigurationProperty
        public void setValue(String value, int index) {
        }

        public String getValue(int index) {
            return null;
        }

    }

}