package hoopoe.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Delegate;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeProfiledInvocation {

    private List<HoopoeProfiledInvocation> children;

    @Getter
    private long totalTimeInNs;

    @Getter
    private long ownTimeInNs;

    @Delegate
    private MethodInfo methodInfo;

    @Getter
    private int invocationsCount;

    public HoopoeProfiledInvocation(String className,
                                    String methodSignature,
                                    List<HoopoeProfiledInvocation> children,
                                    long totalTimeInNs,
                                    long ownTimeInNs,
                                    int invocationsCount,
                                    Collection<HoopoeAttribute> attributes) {
        this.invocationsCount = invocationsCount;
        this.methodInfo = new MethodInfo(className, methodSignature, attributes);
        this.children = new ArrayList<>(children);
        this.totalTimeInNs = totalTimeInNs;
        this.ownTimeInNs = ownTimeInNs;
    }

    public List<HoopoeProfiledInvocation> getChildren() {
        return new ArrayList<>(children);
    }

}
