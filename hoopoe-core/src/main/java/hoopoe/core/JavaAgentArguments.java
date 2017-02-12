package hoopoe.core;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "hoopoe.profiler")
public class JavaAgentArguments {

    private static final String CUSTOM_CONFIG_FILE_PATH_KEY = "hoopoe.configuration.file";

    private Map<String, String> arguments;
    
    public JavaAgentArguments(String agentArgs) {
        log.info("parsing arguments supplied: {}", agentArgs);

        arguments = new HashMap<>();

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
    }

    public String getCustomConfigFilePath() {
        return arguments.get(CUSTOM_CONFIG_FILE_PATH_KEY);
    }
    
}
