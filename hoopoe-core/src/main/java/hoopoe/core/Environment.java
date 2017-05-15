package hoopoe.core;

import java.net.URL;

public class Environment {

    public static final String DEFAULT_CONFIG_PROFILE = "default";

    public URL getDefaultConfigFile() {
        // todo from classpath
        return null;
    }

    public URL getCustomConfigFile() {
        // todo check agent param, fallback to user dir if present, nothing otherwise
        return null;
    }

    public String getConfigurationProfileName() {
        // todo read from agent param, default if none
        return null;
    }
}
