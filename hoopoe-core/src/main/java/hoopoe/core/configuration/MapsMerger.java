package hoopoe.core.configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Merges "customized" map with "master" values.
 * Typical case of default configuration being overridden by custom values.
 * Supports nested merge.
 */
public class MapsMerger {

    /**
     * Merges the maps.
     * If a key is present in master but not in customization, value of master is returned.
     * If a key is present in customization but not in master, value of customization is returned.
     * If a key is present in both, merge applies.
     * {@link Map} values are merged recursively.
     * Other values are overridden. Values should be of the same class.Collections are not merged,
     * but also overridden.
     *
     * @param master        base map, values to be overridden by custom ones, if present.
     * @param customization map to override "master" values.
     *
     * @return new map, which contains all keys from master and customization, with values of the latter to be superior.
     */
    public Map<String, Object> mergeMaps(Map<String, Object> master, Map<String, Object> customization) {
        Map<String, Object> mergedMap = new HashMap<>();

        master.forEach((masterKey, masterValue) -> {
            Object customValue = customization.get(masterKey);
            if (customValue == null) {
                mergedMap.put(masterKey, masterValue);
            } else {
                if (masterValue instanceof Map && customValue instanceof Map) {
                    mergedMap.put(
                            masterKey,
                            mergeMaps((Map) masterValue, (Map) customValue)
                    );
                } else if (!masterValue.getClass().equals(customValue.getClass())) {
                    throw new IllegalArgumentException("Custom value of '" + masterKey + "' has incompatible type");
                } else {
                    mergedMap.put(masterKey, customValue);
                }
            }
        });

        customization.forEach((customKey, customValue) -> {
            if (!mergedMap.containsKey(customKey)) {
                mergedMap.put(customKey, customValue);
            }
        });

        return mergedMap;
    }

}
