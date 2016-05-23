package hoopoe.core;

import hoopoe.utils.HoopoeClassLoader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;

public class HoopoeAgent {

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        ClassLoader currentClassLoader = HoopoeAgent.class.getClassLoader();
        HoopoeClassLoader classLoader = HoopoeClassLoader.fromResource("/hoopoe-core.zip", currentClassLoader);
        Class<?> profilerClass = Class.forName("hoopoe.core.HoopoeProfilerImpl", true, classLoader);
        Constructor<?> constructor = profilerClass.getConstructor(String.class, Instrumentation.class);
        constructor.newInstance(args, instrumentation);
    }

}
