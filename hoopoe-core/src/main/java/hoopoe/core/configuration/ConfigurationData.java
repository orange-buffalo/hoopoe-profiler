package hoopoe.core.configuration;

import hoopoe.core.HoopoeException;
import java.util.Collection;
import java.util.Map;

/**
 * Effectively is a wrapper on a backing map for convenient read of configuration values.
 */
public class ConfigurationData {

    public static final String PROFILE_KEY = "profile";
    public static final String CORE_KEY = "core";

    private final Map<String, Object> configurationData;

    public ConfigurationData(Map<String, Object> configurationData) {
        this.configurationData = configurationData;
    }

    /**
     * Reads the configuration value by provided path.
     * If value is {@code null} or node is missing in the middle of the path, returns {@code defaultValue}.
     * If any intermediate node in the path is a collection, fails.
     * If any of the intermediate nodes is of any other type but map, fails.
     * If {@code expectedValueType} is not assignable from actual value type, fails.
     *
     * @param path              dot-separated path to traverse by.
     * @param expectedValueType type of the value to be expected and validated.
     * @param defaultValue      value to be returned if path does not have the value.
     * @param <T>               type of the value.
     *
     * @return configuration value, default value or throws an exception.
     */
    public <T> T getConfigurationValue(
            String path,
            Class<T> expectedValueType,
            T defaultValue) {

        Object value = readPathValue(path);

        if (value == null) {
            return defaultValue;

        } else {
            Class actualValueType = value.getClass();
            if (!expectedValueType.isAssignableFrom(actualValueType)) {
                throw new HoopoeException("Expected type " + expectedValueType + " does not match actual type " +
                        actualValueType + " for " + path);
            }
            return (T) value;
        }
    }

    private Object readPathValue(String path) {
        String[] pathElements = path.split("\\.");
        Map<String, Object> nextElement = configurationData;
        Object value = null;
        for (int pathElementPos = 0; pathElementPos < pathElements.length; pathElementPos++) {
            String pathElement = pathElements[pathElementPos];
            value = nextElement.get(pathElement);
            if (value == null) {
                break;

            } else if (value instanceof Map) {
                nextElement = (Map<String, Object>) value;

            } else if (value instanceof Collection) {
                throw new HoopoeException("Cannot read " + path + " as it contains collection in the path");

            } else if (pathElementPos < pathElements.length - 1) {
                throw new HoopoeException("Cannot read " + path + " as it is terminated on " + pathElement);
            }
        }
        return value;
    }

}
