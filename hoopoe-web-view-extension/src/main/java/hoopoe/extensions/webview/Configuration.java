package hoopoe.extensions.webview;

import hoopoe.api.configuration.HoopoeConfigurationProperty;
import lombok.Setter;

public class Configuration {

    private static final Integer DEFAULT_PORT = 9786;

    @Setter
    private Integer port;

    @HoopoeConfigurationProperty(name = "Port", description = "Port to start web view at", key = "port")
    public Integer getPort() {
        return port == null ? DEFAULT_PORT : port;
    }
}
