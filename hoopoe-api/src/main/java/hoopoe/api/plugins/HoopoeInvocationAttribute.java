package hoopoe.api.plugins;

/**
 * Describes additional information to be attached to profiled method invocation.
 * <p>
 * Typical example would be a JPQL statement executed in this method.
 */
public interface HoopoeInvocationAttribute {

    /**
     * Distinct name of this information type. Used as a key to aggregate data on.
     * <p>
     * Example: "JPQL Query".
     *
     * @return distinct name of information type; must not be null.
     */
    String getName();

    /**
     * Any additional information to be provided to users. May be also used in aggregations as secondary key.
     * <p>
     * Example: "select Emp from Emp"
     *
     * @return additional information or {@code null} if no additional information exists for this invocation.
     */
    String getDetails();

    /**
     * Determines whether or not this attribute should be accounted in time-based reports and aggregations.
     * @return {@code true} to include this attribute in time-based reporting; {@code false} to exclude.
     */
    boolean isContributingTime();

    /**
     * Creates default implementation, which does not contribute to time-based reports.
     * @param name attribute name
     * @param details attribute details
     * @return default implementation, which does not contribute to time-based reports.
     */
    static HoopoeInvocationAttribute noTimeContribution(String name, String details) {
        return new DefaultHoopoeInvocationAttribute(name, details, false);
    }

    /**
     * Creates default implementation, which contributes to time-based reports.
     * @param name attribute name
     * @param details attribute details
     * @return default implementation, which contributes to time-based reports.
     */
    static HoopoeInvocationAttribute withTimeContribution(String name, String details) {
        return new DefaultHoopoeInvocationAttribute(name, details, true);
    }

}
