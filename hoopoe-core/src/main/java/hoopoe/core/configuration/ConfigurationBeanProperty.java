package hoopoe.core.configuration;

import com.google.common.base.Converter;
import java.lang.reflect.Method;
import lombok.Getter;
import lombok.experimental.Builder;

/**
 * Describes a property of {@link hoopoe.api.configuration.HoopoeConfigurableComponent}.
 * Provides information as specified by user via {@link hoopoe.api.configuration.HoopoeConfigurationProperty}
 * and methods to read and write the values (with embedded conversion between property type and String).
 *
 * @param <T> type of the property.
 */
@Getter
public class ConfigurationBeanProperty<T> {

    private final String name;

    private final String key;

    private final String description;

    private final Method getter;

    private final Method setter;

    private final Converter<String, T> converter;

    @Builder
    public ConfigurationBeanProperty(
            String name,
            String key,
            String description,
            Method getter,
            Method setter,
            Converter<String, T> converter) {

        this.name = name;
        this.key = key;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.converter = converter;
    }
}
