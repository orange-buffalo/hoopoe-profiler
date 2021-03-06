package hoopoe.core.configuration;

import java.lang.reflect.Method;
import lombok.Builder;
import lombok.Getter;

/**
 * Describes a property of {@link hoopoe.api.configuration.HoopoeConfigurableComponent}.
 * Provides information as specified by user via {@link hoopoe.api.configuration.HoopoeConfigurationProperty}
 * and methods to read and write the values.
 *
 * @param <T> type of the property.
 */
@Getter
class ConfigurationBeanProperty<T> {

    private final String name;

    private final String key;

    private final String description;

    private final Method getter;

    private final Method setter;

    private final Class<T> valueType;

    @Builder
    public ConfigurationBeanProperty(
            String name,
            String key,
            String description,
            Method getter,
            Method setter,
            Class<T> valueType) {

        this.name = name;
        this.key = key;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.valueType = valueType;
    }
}
