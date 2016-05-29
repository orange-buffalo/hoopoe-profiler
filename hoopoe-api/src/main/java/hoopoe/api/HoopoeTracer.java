package hoopoe.api;

import java.util.Collection;

public interface HoopoeTracer extends HoopoeProfilerSupplement {

    void onMethodEnter(String className, String methodSignature);

    HoopoeProfiledInvocation onMethodLeave(Collection<HoopoeAttribute> attributes);

}
