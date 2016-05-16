package hoopoe.test.core.supplements;

import hoopoe.api.HoopoeTraceNode;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public abstract class ProfilerTestItem {

    private String description;

    @Setter
    @Getter
    protected Class instrumentedClass;

    public ProfilerTestItem(String description) {
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
}
