package hoopoe.core.configuration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.WordUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses YAML documents to maps.
 */
class YamlDocumentsReader {

    /**
     * Reads and parses input data. For every document in the YAML source generates a map.
     * Resulting map may have nested maps for nested properties.
     * All the keys are normalized to camel-style, i.e. underscore and dashes are converted to camel humps.
     * @param dataStream YAML data.
     * @return collection of maps, every represents a document in YAML source.
     */
    public Collection<Map<String, Object>> readDocuments(InputStream dataStream) {
        Yaml yaml = new Yaml();
        Iterable rawYamlDocuments = yaml.loadAll(dataStream);
        Collection<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> rawYamlDocument : (Iterable<Map<String, Object>>) rawYamlDocuments) {
            Map<String, Object> processedDocument = processRawYamlDocument(rawYamlDocument);
            documents.add(processedDocument);
        }

        return documents;
    }

    private Map<String, Object> processRawYamlDocument(Map<String, Object> rawYaml) {
        Map<String, Object> document = new HashMap<>();
        rawYaml.forEach((compositeRawYamlKey, value) -> {
            String[] rawYamlKeys = compositeRawYamlKey.split("\\.");
            
            Map<String, Object> targetMap = createNestedMapsAndGetTargetMap(document, rawYamlKeys);

            if (value instanceof Map) {
                value = processRawYamlDocument((Map<String, Object>) value);
            }

            String normalizedKey = normalizeYamlKey(rawYamlKeys[rawYamlKeys.length - 1]);
            targetMap.put(normalizedKey, value);
        });
        return document;
    }

    private Map<String, Object> createNestedMapsAndGetTargetMap(Map<String, Object> document, String[] rawYamlKeys) {
        Map<String, Object> targetMap = document;
        for (int i = 0; i < rawYamlKeys.length - 1; i++) {
            String normalizedKey = normalizeYamlKey(rawYamlKeys[i]);
            targetMap = (Map<String, Object>) targetMap.computeIfAbsent(
                    normalizedKey,
                    key -> new HashMap<String, Object>()
            );
        }
        return targetMap;
    }

    private String normalizeYamlKey(String rawYamlKey) {
        String capitalizedKey = WordUtils.capitalize(rawYamlKey, '_', '-');
        capitalizedKey = capitalizedKey.replace("_", "").replace("-", "");
        return capitalizedKey.substring(0, 1).toLowerCase() + capitalizedKey.substring(1);
    }

}
