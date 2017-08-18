package hoopoe.api.plugins;

import java.util.Collection;

/**
 * Records information about method invocation.
 * <p>
 * This will be called <b>after</b> method body invocation, including cases of exceptions.
 * <p>
 * Should be optimized by performance and should never throw exceptions.
 *
 * @see HoopoeInvocationAttribute
 */
public interface HoopoeInvocationRecorder {

    /**
     * Extracts required information from method invocation context and creates zero or more {@link
     * HoopoeInvocationAttribute}.
     *
     * @param arguments    the arguments method was called with; primitive types are represented by their wrappers.
     * @param returnValue  the value method returned in its {@code return} statement. {@code null} for void methods.
     *                     Wrapper for primitive types.
     * @param thisInMethod reference to the object on which method was called; {@code null} for static methods.
     *
     * @return never {@code null}; zero or more {@link HoopoeInvocationAttribute}.
     */
    Collection<HoopoeInvocationAttribute> getAttributes(Object[] arguments, Object returnValue, Object thisInMethod);

}
