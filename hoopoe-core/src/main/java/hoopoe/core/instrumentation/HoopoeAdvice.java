package hoopoe.core.instrumentation;

import hoopoe.core.HoopoeProfilerFacade;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class HoopoeAdvice {

    @Advice.OnMethodEnter
    public static long before() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit
    public static void after(
            @Advice.Enter long startTime,
            @Advice.Origin("#t") String className,
            @MethodSignature String methodSignature,
            @MinimumTrackedTime long minimumTrackedTimeInNs,
            @Advice.AllArguments Object[] arguments,
            @Advice.This(optional = true) Object thisInMethod,
            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
            @PluginRecorders long pluginRecordersReference) throws Exception {

        if (HoopoeProfilerFacade.enabled && HoopoeProfilerFacade.profilingStartTime <= startTime) {
            if (pluginRecordersReference != 0) {
                // if plugin is attached to method, always report it
                HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                        startTime, System.nanoTime(), className, methodSignature,
                        pluginRecordersReference, arguments, returnValue, thisInMethod
                );

            } else {
                long endTime = System.nanoTime();

                if (endTime - startTime >= minimumTrackedTimeInNs) {
                    HoopoeProfilerFacade.methodInvocationProfiler.profileMethodInvocation(
                            startTime, endTime, className, methodSignature, 0, arguments, returnValue, thisInMethod
                    );
                }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MinimumTrackedTime {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface PluginRecorders {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface MethodSignature {
    }
}
