package hoopoe.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeProfiledInvocationRoot {

    @Getter
    private String threadName;

    @Getter
    private HoopoeProfiledInvocation invocation;

    public HoopoeProfiledInvocationRoot(String threadName, HoopoeProfiledInvocation invocation) {
        this.threadName = threadName;
        this.invocation = invocation;
    }

}
