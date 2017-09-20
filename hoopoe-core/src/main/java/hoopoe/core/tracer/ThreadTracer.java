package hoopoe.core.tracer;

import java.util.LinkedList;
import java.util.List;
import lombok.Getter;

public class ThreadTracer {

    @Getter
    private List<TraceNode> traceNodes;

    @Getter
    private String threadName;

    public void clear() {
        traceNodes = null;
        threadName = null;
    }

    public List<TraceNode> init() {
        traceNodes = new LinkedList<>();
        threadName = Thread.currentThread().getName();

        return traceNodes;
    }

}
