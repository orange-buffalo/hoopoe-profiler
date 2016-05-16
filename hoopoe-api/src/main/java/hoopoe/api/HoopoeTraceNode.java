package hoopoe.api;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;
import lombok.experimental.Builder;

public class HoopoeTraceNode {

    @Getter
    private HoopoeTraceNode parent;

    @Getter
    private String className;

    @Getter
    private String methodSignature;

    @Getter
    private Collection<HoopoeTraceNode> children = new ArrayList<>();


    private long startTime;

    private long endTime;

    @Builder
    private HoopoeTraceNode(HoopoeTraceNode parent, String className, String methodSignature) {
        this.parent = parent;
        this.className = className;
        this.methodSignature = methodSignature;
        this.startTime = System.nanoTime();
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public void finish() {
        this.endTime = System.nanoTime();
    }

    public long getDurationInNanoSeconds() {
        return endTime - startTime;
    }
}
