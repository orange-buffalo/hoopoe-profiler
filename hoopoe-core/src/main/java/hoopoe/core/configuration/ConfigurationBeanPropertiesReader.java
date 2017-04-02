package hoopoe.core.configuration;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import hoopoe.api.configuration.HoopoeConfigurationProperty;
import hoopoe.core.HoopoeException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

/**
 * Inspects the class for bean properties annotated with {@link HoopoeConfigurationProperty}.
 */
@Slf4j(topic = "hoopoe.profiler")
public class ConfigurationBeanPropertiesReader {

    private static final Map<Class, Converter> SUPPORTED_CONVERTERS = ImmutableMap.<Class, Converter>builder()
            .put(String.class, Converter.identity())
            .put(Integer.class, Ints.stringConverter())
            .put(Integer.TYPE, Ints.stringConverter())
            .put(Long.class, Longs.stringConverter())
            .put(Long.TYPE, Longs.stringConverter())
            .build();

    @Getter
    public final Collection<ConfigurationBeanProperty> properties;

    public ConfigurationBeanPropertiesReader(Class configurationBeanClass) {
        this.properties = Collections.unmodifiableList(getConfigurationBeanProperties(configurationBeanClass));
    }

    @SneakyThrows
    private List<ConfigurationBeanProperty> getConfigurationBeanProperties(Class configurationBeanClass) {
        BeanInfo configurationBeanInfo = Introspector.getBeanInfo(configurationBeanClass);
        PropertyDescriptor[] javaBeanProperties = configurationBeanInfo.getPropertyDescriptors();
        return Stream.of(javaBeanProperties)
                .map(javaPropertyDescriptor ->
                        createConfigurationBeanProperty(configurationBeanClass, javaPropertyDescriptor))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ConfigurationBeanProperty createConfigurationBeanProperty(
            Class configurationBeanClass, PropertyDescriptor javaPropertyDescriptor) {

        String javaPropertyName = javaPropertyDescriptor.getName();

        log.trace("checking {} on {}", javaPropertyName, configurationBeanClass);

        Method getter = javaPropertyDescriptor.getReadMethod();
        Method setter = javaPropertyDescriptor.getWriteMethod();

        if (getter == null || setter == null) {
            log.trace("property is not read-write, skipping");
            return null;
        }

        HoopoeConfigurationProperty getterConfig = getter.getAnnotation(HoopoeConfigurationProperty.class);
        HoopoeConfigurationProperty setterConfig = setter.getAnnotation(HoopoeConfigurationProperty.class);

        if (getterConfig == null && setterConfig == null) {
            log.trace("property is not annotated, skipping");
            return null;
        }

        if (getterConfig != null && setterConfig != null) {
            throw new HoopoeException(configurationBeanClass.getCanonicalName() +
                    " has annotated getter and setter for property '" + javaPropertyName + "'. This is not supported.");
        }

        HoopoeConfigurationProperty config = (getterConfig == null) ? setterConfig : getterConfig;

        String configPropertyKey = StringUtils.defaultIfBlank(config.key(), javaPropertyName);

        String configPropertyName = config.name();
        if (StringUtils.isBlank(configPropertyName)) {
            configPropertyName = getConfigPropertyName(configPropertyKey);
        }

        return ConfigurationBeanProperty.builder()
                .name(configPropertyName)
                .description(config.description())
                .key(configPropertyKey)
                .setter(setter)
                .getter(getter)
                .converter(getConverter(
                        configurationBeanClass, javaPropertyName, javaPropertyDescriptor.getPropertyType()))
                .build();
    }

    private String getConfigPropertyName(String camelCasePropertyName) {
        return WordUtils.capitalize(
                StringUtils.join(
                        StringUtils.splitByCharacterTypeCamelCase(camelCasePropertyName),
                        " "
                )
        );
    }

    private Converter getConverter(Class configurationBeanClass, String javaPropertyName, Class javaPropertyType) {
        Converter supportedConverter = SUPPORTED_CONVERTERS.get(javaPropertyType);

        if (supportedConverter == null) {
            throw new HoopoeException(configurationBeanClass.getCanonicalName() + " has annotated property '" +
                    javaPropertyName + "' for unsupported property type " + javaPropertyType.getCanonicalName());
        }

        return supportedConverter;
    }

}
