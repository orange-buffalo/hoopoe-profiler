package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.test.core.guineapigs.ApprenticeGuineaPig;
import hoopoe.test.core.guineapigs.BaseGuineaPig;
import hoopoe.test.core.supplements.MethodEntryTestItemDelegate;
import hoopoe.test.core.supplements.SingleThreadProfilerTestItem;
import lombok.experimental.Delegate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".simpleMethod()", 0);
                    }
                },

                new SingleThreadProfilerTestItem("Empty method") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "emptyMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".emptyMethod()", 0);
                    }
                },

                new SingleThreadProfilerTestItem("Method with one call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithOneInnerCall", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".methodWithOneInnerCall()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Method with two calls") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithTwoInnerCalls", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".methodWithTwoInnerCalls()", 2);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));

                        nextNode = traceNode.getChildren().get(1);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".emptyMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Private method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsPrivateMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".callsPrivateMethod()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".privateMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Static method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsStaticMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".callsStaticMethod()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".staticMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Method with params call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsMethodWithParams", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".callsMethodWithParams()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".methodWithParams(int)", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Method with constructor call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithConstructorCall", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".methodWithConstructorCall()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Method with call tree") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithCallTree", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".methodWithCallTree()", 4);
                        long leavesDuration = 0;

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".emptyMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                        leavesDuration += nextNode.getDurationInNanoSeconds();

                        nextNode = traceNode.getChildren().get(1);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "(java.lang.String)", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));

                        nextNode = traceNode.getChildren().get(2);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, ".someSimpleMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                        leavesDuration += nextNode.getDurationInNanoSeconds();

                        nextNode = traceNode.getChildren().get(3);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, ".callBack(hoopoe.test.core.guineapigs.BaseGuineaPig)", 1);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));

                        HoopoeTraceNode callbackNode = nextNode.getChildren().get(0);
                        assertTraceNode(callbackNode, BaseGuineaPig.class, ".methodWithOneInnerCall()", 1);
                        assertThat(callbackNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(nextNode.getDurationInNanoSeconds()));

                        nextNode = callbackNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, ".simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNanoSeconds(),
                                lessThanOrEqualTo(callbackNode.getDurationInNanoSeconds()));
                        leavesDuration += nextNode.getDurationInNanoSeconds();

                        assertThat(leavesDuration, lessThanOrEqualTo(traceNode.getDurationInNanoSeconds()));
                    }
                },

                new SingleThreadProfilerTestItem("Method with exception") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithException", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, ".methodWithException()", 0);
                    }
                }
        );
    }

}
