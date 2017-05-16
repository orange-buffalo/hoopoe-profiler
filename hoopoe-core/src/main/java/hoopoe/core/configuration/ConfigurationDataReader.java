package hoopoe.core.configuration;

import hoopoe.core.Environment;
import hoopoe.core.HoopoeException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Based on application environment setup, reads the configuration files, merges profiles in them,
 * and merges default and custom files. As a result, produces final configuration to be used by Hoopoe.
 */
@Slf4j(topic = "hoopoe.profiler")
public class ConfigurationDataReader {

    private final Environment environment;
    private final YamlDocumentsReader yamlDocumentsReader;
    private final MapsMerger mapsMerger;

    public ConfigurationDataReader(
            Environment environment,
            YamlDocumentsReader yamlDocumentsReader,
            MapsMerger mapsMerger) {

        this.environment = environment;
        this.yamlDocumentsReader = yamlDocumentsReader;
        this.mapsMerger = mapsMerger;
    }

    /**
     * Reads and merges configuration. Uses {@link Environment} to get the default and custom configuration files.
     * Default configuration file is mandatory, while custom one may be optionally provided.
     * If application is started with some profile enabled, will first merge profile configuration into default one,
     * for every file. After that merges custom file data into default file data and return this as a result.
     *
     * @return effective configuration, based on environment setup.
     */
    public Map<String, Object> readConfiguration() {
        URL defaultConfigFile = environment.getDefaultConfigFile();
        if (defaultConfigFile == null) {
            throw new HoopoeException("Default configuration is not defined. Possibly agent is assembled wrongly.");
        }

        String configurationProfileName = environment.getConfigurationProfileName();

        Map<String, Object> defaultConfiguration = readConfigurationFromFile(
                yamlDocumentsReader, defaultConfigFile, mapsMerger, configurationProfileName);

        URL customConfigFile = environment.getCustomConfigFile();
        Map<String, Object> customConfiguration = null;
        if (customConfigFile != null) {
            customConfiguration = readConfigurationFromFile(
                    yamlDocumentsReader, customConfigFile, mapsMerger, configurationProfileName);
        }

        return (customConfiguration == null)
                ? defaultConfiguration
                : mapsMerger.mergeMaps(defaultConfiguration, customConfiguration);
    }

    private Map<String, Object> readConfigurationFromFile(
            YamlDocumentsReader yamlDocumentsReader,
            URL configurationFile,
            MapsMerger mapsMerger,
            String activeProfileName) {

        Collection<Map<String, Object>> configurationDocuments =
                parseConfigurationFile(yamlDocumentsReader, configurationFile);

        Set<String> definedProfiles = new HashSet<>();
        Map<String, Object> defaultProfileConfiguration = null;
        Map<String, Object> customProfileConfiguration = null;
        for (Map<String, Object> configurationDocument : configurationDocuments) {
            String profileName = getConfigurationDocumentProfile(configurationDocument);

            if (!definedProfiles.add(profileName)) {
                return failWithDuplicatedProfile(configurationFile, profileName);
            }

            if (isDefaultProfile(profileName)) {
                defaultProfileConfiguration = configurationDocument;
            } else if (activeProfileName != null && activeProfileName.equals(profileName)) {
                customProfileConfiguration = configurationDocument;
            }
        }

        return mergeConfigurations(
                configurationFile,
                mapsMerger,
                defaultProfileConfiguration,
                customProfileConfiguration);
    }

    private String getConfigurationDocumentProfile(Map<String, Object> configurationDocument) {
        Map<String, Object> coreProperties = (Map<String, Object>) configurationDocument
                .computeIfAbsent(ConfigurationData.CORE_NAMESPACE, key -> new HashMap<>());
        return (String) coreProperties.get(ConfigurationData.PROFILE_KEY);
    }

    private Map<String, Object> failWithDuplicatedProfile(
            URL configurationFile,
            String profileName) {

        if (isDefaultProfile(profileName)) {
            throw new HoopoeException("Default profile was defined multiple times in " + configurationFile);
        } else {
            throw new HoopoeException("Profile " + profileName + " was defined multiple times in " + configurationFile);
        }
    }

    private Map<String, Object> mergeConfigurations(
            URL configurationFile,
            MapsMerger mapsMerger,
            Map<String, Object> defaultProfileConfiguration,
            Map<String, Object> customProfileConfiguration) {

        if (defaultProfileConfiguration != null && customProfileConfiguration != null) {
            return mapsMerger.mergeMaps(defaultProfileConfiguration, customProfileConfiguration);

        } else if (defaultProfileConfiguration != null) {
            return defaultProfileConfiguration;

        } else if (customProfileConfiguration != null) {
            return customProfileConfiguration;

        } else {
            log.warn("no configuration is loaded from {}: neither default nor profile-specific config found",
                    configurationFile);
            return new HashMap<>();
        }
    }

    private boolean isDefaultProfile(String profileName) {
        return profileName == null || Environment.DEFAULT_CONFIG_PROFILE.equals(profileName);
    }

    private Collection<Map<String, Object>> parseConfigurationFile(
            YamlDocumentsReader yamlDocumentsReader,
            URL configurationFile) {

        Collection<Map<String, Object>> configurationDocuments;
        try (InputStream documentsStream = configurationFile.openStream()) {
            configurationDocuments = yamlDocumentsReader.readDocuments(documentsStream);

        } catch (IOException e) {
            throw new HoopoeException("Cannot read " + configurationFile, e);
        }
        return configurationDocuments;
    }
}
