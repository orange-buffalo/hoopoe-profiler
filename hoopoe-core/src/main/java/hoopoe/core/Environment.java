package hoopoe.core;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "hoopoe.profiler")
public class Environment {

    public static final String DEFAULT_CONFIG_PROFILE = "default";

    private static final String CONFIG_PROFILE_KEY = "config.profile";
    private static final String CONFIG_FILE_KEY = "config.file";
    private static final String HOOPOE_DIR = ".hoopoe";
    private static final String HOOPOE_CONFIG_FILE_NAME = "hoopoe-config.yml";

    @Getter
    private URL customConfigFile;

    @Getter
    private String configurationProfileName;

    @Getter
    private URL defaultConfigFile;

    public Environment(String agentArgs) {
        Map<String, String> arguments = parseAgenArguments(agentArgs);
        detectCustomConfigFile(arguments);
        detectConfigurationProfileName(arguments);
        detectDefaultConfigFile();
    }

    private Map<String, String> parseAgenArguments(String agentArgs) {
        log.info("parsing arguments supplied: {}", agentArgs);

        Map<String, String> arguments = new HashMap<>();

        if (StringUtils.isNotBlank(agentArgs)) {
            String[] pairs = agentArgs.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length != 2) {
                    log.error("arguments are not parseable: [{}]; key=value pair [{}] is not valid", agentArgs, pair);
                    throw new IllegalArgumentException("Invalid arguments provided: " + agentArgs);
                }

                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if (key.isEmpty() || value.isEmpty()) {
                    log.error("arguments are not parseable: [{}]; key=value pair [{}] is not valid", agentArgs, pair);
                    throw new IllegalArgumentException("Invalid arguments provided: " + agentArgs);
                }

                arguments.put(key, value);
            }
        }

        log.info("working with arguments: {}", arguments);

        return arguments;
    }

    private void detectDefaultConfigFile() {
        defaultConfigFile = Environment.class.getResource("/" + HOOPOE_CONFIG_FILE_NAME);
    }

    @SneakyThrows
    private void detectCustomConfigFile(Map<String, String> arguments) {
        String agentArgsConfigFilePath = arguments.get(CONFIG_FILE_KEY);
        if (agentArgsConfigFilePath == null) {
            File hoopoeUserDir = new File(FileUtils.getUserDirectory(), HOOPOE_DIR);
            File userDirFile = new File(hoopoeUserDir, HOOPOE_CONFIG_FILE_NAME);
            if (userDirFile.exists()) {
                customConfigFile = userDirFile.toURI().toURL();

                log.info("using custom config file in user directory: {}", customConfigFile);
            }
        } else {
            File agentArgsConfigFile = new File(agentArgsConfigFilePath);
            if (agentArgsConfigFile.exists()) {
                customConfigFile = agentArgsConfigFile.toURI().toURL();

                log.info("using custom config from {}", customConfigFile);
            } else {
                throw new IllegalArgumentException(
                        "Invalid path supplied for custom config file: " + agentArgsConfigFilePath);
            }
        }

        if (customConfigFile == null) {
            log.info("no custom config will be used");
        }
    }

    private void detectConfigurationProfileName(Map<String, String> arguments) {
        configurationProfileName = arguments.get(CONFIG_PROFILE_KEY);
        if (configurationProfileName == null) {
            configurationProfileName = DEFAULT_CONFIG_PROFILE;
        }
    }
}
