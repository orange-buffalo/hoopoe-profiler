package hoopoe.core.bootstrap;

// todo move to a separate module
public abstract class HoopoeProfilerBridge {

    public static HoopoeProfilerBridge instance;

    public static boolean enabled;

    /**
     * Used to cut off long-running threads like in thread executors.
     * Otherwise n-th profiling session can produce results with code started (n - m) sessions ago.
     */
    public static long profilingStartTime;

    public abstract void profileCall(long startTimeInNs,
                                     long endTimeInNs,
                                     String className,
                                     String methodSignature,
                                     Object pluginActionIndicies,
                                     Object[] args,
                                     Object returnValue,
                                     Object thisInMethod);

    public static void startProfiling() {
        enabled = true;
        profilingStartTime = System.nanoTime();
    }

}
