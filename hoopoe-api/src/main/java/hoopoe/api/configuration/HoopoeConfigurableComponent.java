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
     * Creates an instance of configuration class with default configuration values. Will be updated with user-defined
     * values and then passed to {@link HoopoeConfigurableComponent#setCustomizedConfiguration(Object)}.
     *
     * @return configuration class instance with default values.
     */
    T createDefaultConfiguration();

    /**
     * Sets the configuration created by {@link HoopoeConfigurableComponent#createDefaultConfiguration()} and updated
     * with user-defined values.
     *
     * @param configuration updated configuration, default values are overridden with user-defined ones (if any).
     */
    void setCustomizedConfiguration(T configuration);

}
