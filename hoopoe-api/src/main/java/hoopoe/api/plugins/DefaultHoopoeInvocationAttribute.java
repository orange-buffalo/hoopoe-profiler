package hoopoe.api.plugins;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Default attribute implementation.
 */
@ToString
@EqualsAndHashCode
final class DefaultHoopoeInvocationAttribute implements HoopoeInvocationAttribute {

    @Getter
    private String name;

    @Getter
    private String details;

    @Getter
    private boolean contributingTime;

    public DefaultHoopoeInvocationAttribute(String name, String details, boolean contributingTime) {
        this.name = name;
        this.details = details;
        this.contributingTime = contributingTime;
    }
}
