package hoopoe.api.configuration;

/**
 * Indicates a component (plugin, extension etc) that has its own configuration.
 * <p>
 * Any component of this type will be updated with custom configuration after creation and before execution.
 *
 * @param <T> configuration class. This class will be inspected for suitable configuration properties. A bean property
 *            is a configuration property if either its getter or setter is annotated with {@link
 *            HoopoeConfigurationProperty}. In case when both getter and setter are annotated, exception will be thrown.
 *            Read-only or write-only properties are not supported.
 */
public interface HoopoeConfigurableComponent<T> {

    /**
     * Returns the configuration of this component. Every configuration property in this object will be updated with
     * user-defined values, unless the user-provided value is {@code null} (which actually means user decided to keep
     * the default).
     * <p>
     * Typical use case would be to create this object with default configuration values in component's constructor and
     * then let Hoopoe override any values provided by the user.
     *
     * @return configuration object for this component; never {@code null}.
     */
    T getConfiguration();

}
