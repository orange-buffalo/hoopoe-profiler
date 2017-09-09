package hoopoe.core;

/**
 * This class is deployed to bootstrap classloader in order to be visible to any class instrumented by Hoopoe Profiler.
 * Code added by instrumentation uses this class to delegate profiling code to profiler implementation. This class has
 * no other dependencies to not pollute bootstrap classloader.
 */
public abstract class HoopoeProfilerFacade {

    public static HoopoeProfilerFacade instance;

    public static boolean enabled;

    /**
     * Used to cut off long-running threads like in thread executors. Otherwise n-th profiling session can produce
     * results with code started (n - m) sessions ago.
     */
    public static long profilingStartTime;

    public abstract void profileCall(
            long startTimeInNs,
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
