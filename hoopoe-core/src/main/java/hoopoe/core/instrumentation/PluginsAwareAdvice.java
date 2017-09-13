package hoopoe.core.instrumentation;

import hoopoe.core.HoopoeProfilerFacade;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

class PluginsAwareAdvice {

    @Advice.OnMethodExit
    public static void after(@Advice.Enter long startTime,
                             @ClassName String className,
                             @MethodSignature String methodSignature,
                             @MinimumTrackedTime long minimumTrackedTimeInNs,
                             @Advice.AllArguments Object[] arguments,
                             @Advice.This(optional = true) Object thisInMethod,
                             @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
                             @PluginActions Object pluginActionIndicies) throws Exception {

        if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
            // if plugin is attached to method, always report it
            HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                    startTime, System.nanoTime(), className, methodSignature,
                    pluginActionIndicies, arguments, returnValue, thisInMethod
            );
        }
    }
}