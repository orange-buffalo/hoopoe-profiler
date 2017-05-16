package hoopoe.core.configuration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Describes a plugin or an extension, which is enabled via configuration.
 */
@EqualsAndHashCode
@ToString
public class EnabledComponentData {

    /**
     * ID of this component, as it is defined in configuration. For example, {@code plugins.sql-plugin.enabled: true}
     * describes a plugin with ID {@code sqlPlugin}.
     */
    @Getter
    private String id;

    /**
     * Path to the component's ZIP. To be loaded by application into isolated child classloader.
     */
    @Getter
    private String binariesPath;

    public EnabledComponentData(String id, String binariesPath) {
        this.id = id;
        this.binariesPath = binariesPath;
    }
}
