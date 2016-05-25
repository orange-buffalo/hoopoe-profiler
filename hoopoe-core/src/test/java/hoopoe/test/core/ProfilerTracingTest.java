package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.test.core.guineapigs.ApprenticeGuineaPig;
import hoopoe.test.core.guineapigs.BaseGuineaPig;
import hoopoe.test.core.guineapigs.RunnableGuineaPig;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import hoopoe.test.core.supplements.MethodEntryTestItemDelegate;
import hoopoe.test.core.supplements.ProfilerTraceTestItem;
import hoopoe.test.core.supplements.SingleThreadProfilerTraceTestItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

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
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Empty method") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "emptyMethod", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "emptyMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with one call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithOneInnerCall", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "methodWithOneInnerCall()",
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with two calls") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithTwoInnerCalls", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "methodWithTwoInnerCalls()",
                                BaseGuineaPig.class, "simpleMethod()",
                                BaseGuineaPig.class, "emptyMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Private method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsPrivateMethod", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "callsPrivateMethod()",
                                BaseGuineaPig.class, "privateMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Static method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsStaticMethod", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "callsStaticMethod()",
                                BaseGuineaPig.class, "staticMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with params call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsMethodWithParams", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "callsMethodWithParams()",
                                BaseGuineaPig.class, "methodWithParams(int)");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with constructor call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithConstructorCall", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "methodWithConstructorCall()",
                                ApprenticeGuineaPig.class, "ApprenticeGuineaPig()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with call tree") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithCallTree", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "methodWithCallTree()",
                                BaseGuineaPig.class, "emptyMethod()",
                                ApprenticeGuineaPig.class, "ApprenticeGuineaPig(java.lang.String)",
                                ApprenticeGuineaPig.class, "someSimpleMethod()",
                                ApprenticeGuineaPig.class, "callBack(hoopoe.test.core.guineapigs.BaseGuineaPig)",
                                BaseGuineaPig.class, "methodWithOneInnerCall()",
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with exception") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithException", this);

                    @Override
                    protected void assertCapturedInvocation(List<CapturedInvocation> invocations) {
                        assertInvocationSequence(invocations,
                                BaseGuineaPig.class, "methodWithException()");
                    }
                },

                new ProfilerTraceTestItem("Child thread") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "startNewThread", this);

                    @Override
                    public void assertCapturedData(String originalThreadName,
                                                   Map<String, List<ProfilerTracingTest.CapturedInvocation>> capturedData) {
                        assertThat(capturedData.size(), equalTo(2));

                        List<CapturedInvocation> mainThreadInvocation = capturedData.get(originalThreadName);
                        assertThat(mainThreadInvocation, notNullValue());
                        assertInvocationSequence(mainThreadInvocation,
                                BaseGuineaPig.class, "startNewThread()",
                                RunnableGuineaPig.class, "RunnableGuineaPig()");

                        List<CapturedInvocation> childThreadInvocation = capturedData.get("RunnableGuineaPig");
                        assertThat(mainThreadInvocation, notNullValue());
                        assertInvocationSequence(childThreadInvocation,
                                RunnableGuineaPig.class, "run()",
                                RunnableGuineaPig.class, "innerMethod()");
                    }
                }
        );
    }

    @Test
    @UseDataProvider("dataForProfilingTest")
    public void testProfiling(ProfilerTraceTestItem testItem) throws Exception {
        HoopoeTestClassLoader classLoader = new HoopoeTestClassLoader("hoopoe.test.core.guineapigs");

        // load before agent is connected to avoid infinite recursion
        CapturedInvocation.class.getName();

        Map<String, List<CapturedInvocation>> capturedData = new HashMap<>();
        doAnswer(
                invocation -> {
                    Object[] arguments = invocation.getArguments();
                    String className = (String ) arguments[0];
                    String methodSignature = (String) arguments[1];
                    String threadName = Thread.currentThread().getName();
                    List<CapturedInvocation> capturedInvocations = capturedData.get(threadName);
                    if (capturedInvocations == null) {
                        capturedInvocations = new ArrayList<>();
                        capturedData.put(threadName, capturedInvocations);
                    }
                    capturedInvocations.add(new CapturedInvocation(className, methodSignature));
                    return null;
                })
                .when(HoopoeTestConfiguration.getTracerMock())
                .onMethodEnter(any(), any());
        when(HoopoeTestConfiguration.getTracerMock().onMethodLeave()).thenReturn(null);

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

    @Getter
    @AllArgsConstructor
    public static class CapturedInvocation {
        protected String className;
        protected String methodSignature;
    }

}