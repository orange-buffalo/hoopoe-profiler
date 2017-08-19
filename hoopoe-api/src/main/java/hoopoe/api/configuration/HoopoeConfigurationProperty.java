package hoopoe.api.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Improves Hoopoe configuration integration.
 * <p/>
 * Provides human-readable name and description of configuration property as well as technical key of this property.
 * Applicable for getters and setters of component configuration class.
 *
 * @see HoopoeConfigurableComponent
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface HoopoeConfigurationProperty {

    /**
     * Provides the name of the property in human-readable presentation, to be displayed to users.
     *
     * @return property name, or empty string to use internal naming strategy.
     */
    String name() default "";

    /**
     * Provides the description of the property in human-readable presentation, to be displayed to users.
     *
     * @return property description, or empty string to indicate that there is no additional information to be provided
     * to users.
     */
    String description() default "";

    /**
     * Provides the technical key of the property. To be used to read-write the value from/to the configuration sources.
     * If {@link HoopoeConfigurationProperty#name()} is empty, will be used to display the property to users.
     *
     * @return property technical name, or empty string to use internal naming strategy. May be a composite key, i.e
     * "server.port"
     */
    String key() default "";

}
