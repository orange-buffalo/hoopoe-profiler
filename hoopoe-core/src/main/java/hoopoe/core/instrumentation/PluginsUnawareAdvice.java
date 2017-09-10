package hoopoe.core.instrumentation;

import hoopoe.core.HoopoeProfilerFacade;
import net.bytebuddy.asm.Advice;

class PluginsUnawareAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void after(@Advice.Enter long startTime,
                             @ClassName String className,
                             @MethodSignature String methodSignature,
                             @MinimumTrackedTime long minimumTrackedTimeInNs) throws Exception {

        if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
            long endTime = System.nanoTime();

            if (endTime - startTime >= minimumTrackedTimeInNs) {
                HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                        startTime, endTime, className, methodSignature, null, null, null, null
                );
            }
        }
    }
}
