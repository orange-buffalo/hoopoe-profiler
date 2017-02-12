package hoopoe.core.configuration;

import lombok.Getter;

@Getter
public class EnabledComponentData {

    private String pluginId;
    private String binariesSource;

    public EnabledComponentData(String pluginId, String binariesSource) {
        this.pluginId = pluginId;
        this.binariesSource = binariesSource;
    }
}
