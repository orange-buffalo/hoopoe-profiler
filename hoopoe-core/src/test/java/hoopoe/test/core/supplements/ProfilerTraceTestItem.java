package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeTraceNode;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ProfilerTraceTestItem {

    private String description;

    @Setter
    @Getter
    protected Class instrumentedClass;

    public ProfilerTraceTestItem(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public abstract Class getEntryPointClass();

    public abstract void prepareTest() throws Exception;

    public abstract void executeTest() throws Exception;

    public abstract void assertCapturedData(String originalThreadName,
                                            Map<String, HoopoeTraceNode> capturedData);

    protected void assertTraceNode(HoopoeTraceNode traceNode, Class clazz, String methodSignature, int childrenCount) {
        String className = clazz.getCanonicalName();
        assertThat(traceNode.getClassName(), equalTo(className));
        assertThat(traceNode.getMethodSignature(), equalTo(methodSignature));
        assertThat(traceNode.getChildren().size(), equalTo(childrenCount));
    }
}
