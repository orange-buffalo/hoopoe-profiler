package hoopoe.tests;

import hoopoe.utils.HoopoeClassLoader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * Java Agent for integration tests.
 */
public class HoopoeTestAgent {

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        ClassLoader currentClassLoader = HoopoeTestAgent.class.getClassLoader();
        HoopoeClassLoader hoopoeClassLoader = HoopoeClassLoader.fromResource("/hoopoe-core.zip", currentClassLoader);
        Class<?> bootstrapperClass = Class.forName("hoopoe.core.HoopoeBootstrapper", true, hoopoeClassLoader);
        Method bootstrapMethod = bootstrapperClass.getMethod("bootstrapHoopoe", String.class, Instrumentation.class);
        bootstrapMethod.invoke(null, args, instrumentation);
    }

}
