package hoopoe.api;

public interface HoopoeTracer extends HoopoeProfilerSupplement {

    HoopoeHasAttributes onMethodEnter(String className, String methodSignature);

    HoopoeProfiledInvocation onMethodLeave();

}
