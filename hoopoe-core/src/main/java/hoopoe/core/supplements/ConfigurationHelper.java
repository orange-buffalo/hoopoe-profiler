package hoopoe.core.supplements;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.core.HoopoeConfigurationImpl;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j(topic = "hoopoe.profiler")
public class ConfigurationHelper {

    private static final String CONFIGURATOR_KEY = "hoopoe.configuration.class";

    public HoopoeConfiguration getConfiguration(String rawArgs) {
        Properties arguments = parseArguments(rawArgs);
        return initConfiguration(arguments);
    }

    private Properties parseArguments(String rawArgs) {
        log.info("parsing arguments supplied: {}", rawArgs);

        Properties arguments = new Properties();

        if (StringUtils.isNotBlank(rawArgs)) {
            String[] pairs = rawArgs.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length != 2) {
                    log.error("arguments are not parseable: [{}]; key=value pair [{}] is not valid", rawArgs, pair);
                    throw new IllegalArgumentException("Invalid arguments provided");
                }

                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if (key.isEmpty() || value.isEmpty()) {
                    log.error("arguments are not parseable: [{}]; key=value pair [{}] is not valid", rawArgs, pair);
                    throw new IllegalArgumentException("Invalid arguments provided");
                }

                arguments.put(key, value);
            }
        }

        log.info("working with arguments: {}", arguments);

        return arguments;
    }

    private HoopoeConfiguration initConfiguration(Properties arguments) {
        String configurationClassName = arguments.getProperty(CONFIGURATOR_KEY);

        if (configurationClassName == null) {
            log.info("initializing default configuration");
            return new HoopoeConfigurationImpl();
        }

        log.info("initializing configurator: {}", configurationClassName);
        try {
            Class<? extends HoopoeConfiguration> configurationClass =
                    (Class<? extends HoopoeConfiguration>) Class.forName(configurationClassName);

            return configurationClass.newInstance();
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("cannot instantiate configuration", e);
            throw new IllegalArgumentException(e);
        }
    }
}
