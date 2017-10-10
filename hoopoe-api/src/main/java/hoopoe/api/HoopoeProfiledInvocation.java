package hoopoe.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoopoeProfiledInvocation {

    private List<HoopoeProfiledInvocation> children = new ArrayList<>(); //todo lazy;

    @Getter
    private long totalTimeInNs;

    @Getter
    private long ownTimeInNs;

    @Getter
    private String className;

    @Getter
    private String methodSignature;

    @Getter
    private int invocationsCount;

    private Collection<HoopoeInvocationAttribute> attributes = new ArrayList<>(); //todo lazy

    public HoopoeProfiledInvocation(String className,
                                    String methodSignature,
                                    List<HoopoeProfiledInvocation> children,
                                    long totalTimeInNs,
                                    long ownTimeInNs,
                                    int invocationsCount,
                                    Collection<HoopoeInvocationAttribute> attributes) {
        this.invocationsCount = invocationsCount;
        this.className = className;
        this.methodSignature = methodSignature;
        this.attributes = attributes;
        this.children = new ArrayList<>(children);
        this.totalTimeInNs = totalTimeInNs;
        this.ownTimeInNs = ownTimeInNs;
    }

    public List<HoopoeProfiledInvocation> getChildren() {
        return new ArrayList<>(children);
    }

    public Stream<HoopoeProfiledInvocation> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(HoopoeProfiledInvocation::flattened));
    }

    public Collection<HoopoeInvocationAttribute> getAttributes() {
        return new ArrayList<>(attributes);
    }

    @Override
    public String toString() {
        return className + "." + methodSignature + " [" + totalTimeInNs + "ns]";
    }

}
