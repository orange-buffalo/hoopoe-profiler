package hoopoe.core.bootstrap;

// todo move to a separate module
public abstract class HoopoeProfilerBridge {

    public static HoopoeProfilerBridge instance;

    public static boolean enabled;

    public abstract void profileCall(long startTimeInNs,
                                     long endTimeInNs,
                                     String className,
                                     String methodSignature,
                                     int[] pluginActionIndicies,
                                     Object[] args,
                                     Object returnValue,
                                     Object thisInMethod);

}
