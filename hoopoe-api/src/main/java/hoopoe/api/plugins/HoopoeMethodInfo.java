package hoopoe.api.plugins;

import java.util.Collection;

/**
 * Describes Java method, including hosting class, its superclasses and interfaces, method arguments.
 *
 * @see HoopoePlugin#createActionIfSupported(HoopoeMethodInfo)
 */
public interface HoopoeMethodInfo {

    /**
     * Returns canonical class name of hosting class.
     *
     * @return canonical class name of hosting class.
     */
    String getCanonicalClassName();

    /**
     * Returns method signature in format: {@code <method_name>(<argument1_type>,<argument2_type,...,<argumentN_type)}.
     * <p>
     * For constructors, class name is used as method name.
     * <p>
     * Every argument type is a canonical Java class name or primitive type name. For array arguments {@code []} is
     * added after type name. Argument types do not include generic parameters.
     * <p>
     * Examples: <p> {@code stringMethod(java.lang.String)} <p> {@code intArrayMethod(int[])} <p> {@code
     * stringArrayMethod(java.lang.String[])} <p> {@code collectionMethod(java.util.List)} <p> {@code
     * multipleArgumentsMethod(java.lang.String,int[])}
     *
     * @return method signature.
     */
    String getMethodSignature();

    /**
     * Returns canonical names of all superclasses of hosted class and all interfaces it implements (including indirect
     * ones).
     *
     * @return canonical class names of all superclasses and interfaces of hosted class.
     */
    Collection<String> getSuperclasses();

    /**
     * Verifies if hosted class is of specified type.
     *
     * @param className canonical class name to check
     *
     * @return {@code true} if hosted class is {@code className}, implements {@code className} or extends {@code
     * className} class; {code false} otherwise.
     */
    boolean isInstanceOf(String className);
}
