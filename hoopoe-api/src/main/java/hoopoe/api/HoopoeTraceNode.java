package hoopoe.api;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Delegate;

public class HoopoeTraceNode {

    @Getter
    private HoopoeTraceNode parent;

    private List<HoopoeTraceNode> children = new ArrayList<>();

    private long startTime;

    private long endTime;

    @Delegate
    private MethodInfo methodInfo;

    public HoopoeTraceNode(HoopoeTraceNode parent, String className, String methodSignature) {
        this.parent = parent;
        this.methodInfo = new MethodInfo(className, methodSignature);
        this.startTime = System.nanoTime();
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public void finish() {
        this.endTime = System.nanoTime();
    }

    public long getDurationInNs() {
        return endTime - startTime;
    }

    public List<HoopoeTraceNode> getChildren() {
        return new ArrayList<>(children);
    }

}
