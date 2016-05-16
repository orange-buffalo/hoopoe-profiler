package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.test.core.guineapigs.BaseGuineaPig;
import hoopoe.test.core.supplements.MethodEntryTestItemDelegate;
import hoopoe.test.core.supplements.SingleThreadProfilerTestItem;
import lombok.experimental.Delegate;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ProfilerTest extends AbstractProfilerTest {

    @DataProvider
    public static Object[][] dataForProfilingTest() {
        return transform(
                new SingleThreadProfilerTestItem("Simple method with no other calls") {

                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "simpleMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        String className = BaseGuineaPig.class.getCanonicalName();
                        assertThat(traceNode.getClassName(), equalTo(className));
                        assertThat(traceNode.getMethodSignature(), equalTo(className + ".simpleMethod()"));
                        assertThat(traceNode.getChildren().size(), equalTo(0));
                    }
                },

                new SingleThreadProfilerTestItem("Empty method") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "emptyMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        String className = BaseGuineaPig.class.getCanonicalName();
                        assertThat(traceNode.getClassName(), equalTo(className));
                        assertThat(traceNode.getMethodSignature(), equalTo(className + ".emptyMethod()"));
                        assertThat(traceNode.getChildren().size(), equalTo(0));
                    }
                }
        );
    }

}
