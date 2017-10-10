package hoopoe.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Describes additional information to be attached to profiled method invocation.
 * <p>
 * Typical example would be a JPQL statement executed in this method.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeInvocationAttribute {

    /**
     * Distinct name of this information type. Used as a key to aggregate data on.
     * <p>
     * Example: "JPQL Query".
     *
     * @return distinct name of information type; must not be null.
     */
    @Getter
    private String name;

    /**
     * Any additional information to be provided to users. May be also used in aggregations as secondary key.
     * <p>
     * Example: "select Emp from Emp"
     *
     * @return additional information or {@code null} if no additional information exists for this invocation.
     */
    @Getter
    private String details;

    /**
     * Determines whether or not this attribute should be accounted in time-based reports and aggregations.
     *
     * @return {@code true} to include this attribute in time-based reporting; {@code false} to exclude.
     */
    @Getter
    private boolean contributingTime;

    private HoopoeInvocationAttribute(String name, String details, boolean contributingTime) {
        this.name = name;
        this.details = details;
        this.contributingTime = contributingTime;
    }

    /**
     * Creates default implementation, which does not contribute to time-based reports.
     *
     * @param name    attribute name
     * @param details attribute details
     *
     * @return default implementation, which does not contribute to time-based reports.
     */
    public static HoopoeInvocationAttribute noTimeContribution(String name, String details) {
        return new HoopoeInvocationAttribute(name, details, false);
    }

    /**
     * Creates default implementation, which contributes to time-based reports.
     *
     * @param name    attribute name
     * @param details attribute details
     *
     * @return default implementation, which contributes to time-based reports.
     */
    public static HoopoeInvocationAttribute withTimeContribution(String name, String details) {
        return new HoopoeInvocationAttribute(name, details, true);
    }

}
