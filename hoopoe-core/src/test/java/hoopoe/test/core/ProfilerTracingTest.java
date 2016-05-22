package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.test.core.guineapigs.ApprenticeGuineaPig;
import hoopoe.test.core.guineapigs.BaseGuineaPig;
import hoopoe.test.core.guineapigs.RunnableGuineaPig;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import hoopoe.test.core.supplements.MethodEntryTestItemDelegate;
import hoopoe.test.core.supplements.ProfilerTraceTestItem;
import hoopoe.test.core.supplements.SingleThreadProfilerTraceTestItem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.experimental.Delegate;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(DataProviderRunner.class)
public class ProfilerTracingTest extends AbstractProfilerTest {

    @DataProvider
    public static Object[][] dataForProfilingTest() {
        return transform(
                new SingleThreadProfilerTraceTestItem("Simple method with no other calls") {

                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "simpleMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "simpleMethod()", 0);
                    }
                },

                new SingleThreadProfilerTraceTestItem("Empty method") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "emptyMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "emptyMethod()", 0);
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with one call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithOneInnerCall", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "methodWithOneInnerCall()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with two calls") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithTwoInnerCalls", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "methodWithTwoInnerCalls()", 2);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));

                        nextNode = traceNode.getChildren().get(1);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "emptyMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Private method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsPrivateMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "callsPrivateMethod()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "privateMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Static method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsStaticMethod", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "callsStaticMethod()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "staticMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with params call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsMethodWithParams", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "callsMethodWithParams()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "methodWithParams(int)", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with constructor call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithConstructorCall", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "methodWithConstructorCall()", 1);

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "ApprenticeGuineaPig()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with call tree") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithCallTree", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "methodWithCallTree()", 4);
                        long leavesDuration = 0;

                        HoopoeTraceNode nextNode = traceNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "emptyMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                        leavesDuration += nextNode.getDurationInNs();

                        nextNode = traceNode.getChildren().get(1);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "ApprenticeGuineaPig(java.lang.String)", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));

                        nextNode = traceNode.getChildren().get(2);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "someSimpleMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));
                        leavesDuration += nextNode.getDurationInNs();

                        nextNode = traceNode.getChildren().get(3);
                        assertTraceNode(nextNode, ApprenticeGuineaPig.class, "callBack(hoopoe.test.core.guineapigs.BaseGuineaPig)", 1);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(traceNode.getDurationInNs()));

                        HoopoeTraceNode callbackNode = nextNode.getChildren().get(0);
                        assertTraceNode(callbackNode, BaseGuineaPig.class, "methodWithOneInnerCall()", 1);
                        assertThat(callbackNode.getDurationInNs(),
                                lessThanOrEqualTo(nextNode.getDurationInNs()));

                        nextNode = callbackNode.getChildren().get(0);
                        assertTraceNode(nextNode, BaseGuineaPig.class, "simpleMethod()", 0);
                        assertThat(nextNode.getDurationInNs(),
                                lessThanOrEqualTo(callbackNode.getDurationInNs()));
                        leavesDuration += nextNode.getDurationInNs();

                        assertThat(leavesDuration, lessThanOrEqualTo(traceNode.getDurationInNs()));
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with exception") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithException", this);

                    @Override
                    protected void assertCapturedTraceNode(HoopoeTraceNode traceNode) {
                        assertTraceNode(traceNode, BaseGuineaPig.class, "methodWithException()", 0);
                    }
                },

                new ProfilerTraceTestItem("Child thread") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "startNewThread", this);

                    @Override
                    public void assertCapturedData(String originalThreadName,
                                                   Map<String, HoopoeTraceNode> capturedData) {
                        assertThat(capturedData.size(), equalTo(2));

                        HoopoeTraceNode mainThreadNode = capturedData.get(originalThreadName);
                        assertThat(mainThreadNode, notNullValue());
                        assertTraceNode(mainThreadNode, BaseGuineaPig.class, "startNewThread()", 1);

                        HoopoeTraceNode nextNode = mainThreadNode.getChildren().get(0);
                        assertTraceNode(nextNode, RunnableGuineaPig.class, "RunnableGuineaPig()", 0);

                        HoopoeTraceNode childThreadNode = capturedData.get("RunnableGuineaPig");
                        assertThat(mainThreadNode, notNullValue());
                        assertTraceNode(childThreadNode, RunnableGuineaPig.class, "run()", 1);

                        nextNode = childThreadNode.getChildren().get(0);
                        assertTraceNode(nextNode, RunnableGuineaPig.class, "innerMethod()", 0);
                    }
                }
        );
    }

    @Test
    @UseDataProvider("dataForProfilingTest")
    public void testProfiling(ProfilerTraceTestItem testItem) throws Exception {
        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader();

        ConcurrentMap<String, HoopoeTraceNode> capturedData = new ConcurrentHashMap<>();
        Mockito.doAnswer(
                invocation -> {
                    Object[] arguments = invocation.getArguments();
                    Thread thread = (Thread) arguments[0];
                    HoopoeTraceNode node = (HoopoeTraceNode) arguments[1];
                    capturedData.putIfAbsent(thread.getName(), node);
                    return null;
                })
                .when(HoopoeTestConfiguration.getStorageMock())
                .consumeThreadTraceResults(Mockito.any(), Mockito.any());

        String threadName = "testThread" + System.nanoTime();
        executeWithAgentLoaded(() -> {
            Class instrumentedClass = classLoader.loadClass(testItem.getEntryPointClass().getCanonicalName());
            testItem.setInstrumentedClass(instrumentedClass);
            testItem.prepareTest();

            Thread thread = new Thread(() -> {
                try {
                    testItem.executeTest();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, threadName);
            thread.start();
            thread.join();
        });

        // all captured executions in this thread are preparations and should be ignored during assertion
        capturedData.remove(Thread.currentThread().getName());
        testItem.assertCapturedData(threadName, capturedData);
    }

}