package hoopoe.core.bootstrap;

// todo move to a separate module
public abstract class HoopoeProfilerBridge {

    public static final int[] NO_PLUGINS = new int[0];

    public static HoopoeProfilerBridge instance;

    public abstract void startMethodProfiling(String className,
                                              String methodSignature);

    public abstract void finishMethodProfiling(int[] pluginActionIndicies,
                                               Object[] args,
                                               Object returnValue,
                                               Object thisInMethod);

}
