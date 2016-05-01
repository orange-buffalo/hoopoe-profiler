package hoopoe.core;

import hoopoe.api.HoopoeConfigurator;
import static hoopoe.core.HoopoeProfiler.LOG_CATEGORY;
import java.lang.instrument.Instrumentation;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LOG_CATEGORY)
public class HoopoeProfiler {

    public static final String LOG_CATEGORY = "hoopoe.profiler";
    private static final String CONFIGURATOR_KEY = "hoopoe.configurator.class";

    public static void init(String rawArgs, Instrumentation instrumentation) {
        log.info("starting profile configuration");

        Properties arguments = parseArguments(rawArgs);

        HoopoeConfigurator configurator = initConfigurator(arguments.getProperty(CONFIGURATOR_KEY));






        instrumentation.addTransformer(new HoopoeClassFileTransformer());
    }

    private static HoopoeConfigurator initConfigurator(String configuratorClassName) {
        if (configuratorClassName == null) {
            log.info("initializing default configurator");
            return new DefaultConfigurator();
        }

        log.info("initializing configurator: {}");
        try {
            Class<? extends HoopoeConfigurator> configuratorClass =
                    (Class<? extends HoopoeConfigurator>) Class.forName(configuratorClassName);

            return configuratorClass.newInstance();
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("cannot instantiate configurator", e);
            throw new IllegalArgumentException(e);
        }
    }

    public static void beforeMethod(String className, String methodSignature, Object[] args) {
        log.info("tada");
    }

    public static void afterMethod(String className, String methodSignature, Object[] args) {

        log.info("tada2");
    }

    private static Properties parseArguments(String rawArgs) {
        log.info("parsing arguments supplied: {}", rawArgs);

        Properties arguments = new Properties();

        if (rawArgs != null) {
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

        return arguments;
    }

}
