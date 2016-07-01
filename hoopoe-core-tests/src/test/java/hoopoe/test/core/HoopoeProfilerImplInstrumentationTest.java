package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.test.core.guineapigs.ApprenticeGuineaPig;
import hoopoe.test.core.guineapigs.BaseGuineaPig;
import hoopoe.test.core.guineapigs.RunnableGuineaPig;
import hoopoe.test.core.supplements.MethodEntryTestItemDelegate;
import hoopoe.test.core.supplements.ProfilerTraceTestItem;
import hoopoe.test.core.supplements.SingleThreadProfilerTraceTestItem;
import hoopoe.test.supplements.HoopoeTestExecutor;
import hoopoe.test.supplements.HoopoeTestHelper;
import static hoopoe.test.supplements.HoopoeTestHelper.msToNs;
import hoopoe.test.supplements.TestConfiguration;
import hoopoe.test.supplements.TestConfigurationRule;
import java.util.Map;
import lombok.experimental.Delegate;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class HoopoeProfilerImplInstrumentationTest {

    @Rule
    public TestConfigurationRule configurationRule = new TestConfigurationRule();

    @DataProvider
    public static Object[][] dataForProfilingTest() {
        return HoopoeTestHelper.transform(
                new SingleThreadProfilerTraceTestItem("Simple method with no other calls") {

                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "simpleMethod", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Empty method") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "emptyMethod", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "emptyMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with one call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithOneInnerCall", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "methodWithOneInnerCall()",
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with two calls") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithTwoInnerCalls", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "methodWithTwoInnerCalls()",
                                BaseGuineaPig.class, "emptyMethod()",
                                BaseGuineaPig.class, "simpleMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Private method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsPrivateMethod", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "callsPrivateMethod()",
                                BaseGuineaPig.class, "privateMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Static method call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsStaticMethod", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "callsStaticMethod()",
                                BaseGuineaPig.class, "staticMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with params call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsMethodWithParams", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "callsMethodWithParams()",
                                BaseGuineaPig.class, "methodWithParams(int)");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with constructor call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithConstructorCall", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "methodWithConstructorCall()",
                                ApprenticeGuineaPig.class, "ApprenticeGuineaPig()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with call tree") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "methodWithCallTree", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
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
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "methodWithException()");
                    }
                },

                new ProfilerTraceTestItem("Child thread") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "startNewThread", this);

                    @Override
                    public void assertCapturedData(String originalThreadName,
                                                   Map<String, HoopoeProfiledInvocation> capturedData) {
                        assertThat(capturedData.size(), equalTo(2));

                        HoopoeProfiledInvocation mainThreadInvocation = capturedData.get(originalThreadName);
                        assertThat(mainThreadInvocation, notNullValue());
                        assertInvocationSequence(mainThreadInvocation,
                                BaseGuineaPig.class, "startNewThread()",
                                RunnableGuineaPig.class, "RunnableGuineaPig()");

                        HoopoeProfiledInvocation childThreadInvocation = capturedData.get("RunnableGuineaPig");
                        assertThat(mainThreadInvocation, notNullValue());
                        assertInvocationSequence(childThreadInvocation,
                                RunnableGuineaPig.class, "run()",
                                RunnableGuineaPig.class, "innerMethod()");
                    }
                },

                new SingleThreadProfilerTraceTestItem("Method with multiple params call") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "callsMethodWithMultipleParams", this);

                    @Override
                    protected void assertCapturedInvocation(HoopoeProfiledInvocation actualInvocations) {
                        assertInvocationSequence(actualInvocations,
                                BaseGuineaPig.class, "callsMethodWithMultipleParams()",
                                BaseGuineaPig.class, "methodWithMultipleParams(java.lang.String,int[],java.lang.Object[])");
                    }
                },

                new ProfilerTraceTestItem("Fast invocations should be trimmed") {
                    @Delegate
                    MethodEntryTestItemDelegate delegate =
                            new MethodEntryTestItemDelegate(BaseGuineaPig.class, "simpleMethod", this);

                    @Override
                    public void setupConfiguration() {
                        TestConfiguration.setMinimumTrackedInvocationTimeInNs(msToNs(10_000));
                    }

                    @Override
                    public void assertCapturedData(String originalThreadName,
                                                   Map<String, HoopoeProfiledInvocation> capturedData) {
                        assertThat(capturedData.size(), equalTo(0));
                    }
                }
        );
    }

    @Test
    @UseDataProvider("dataForProfilingTest")
    public void testProfiling(ProfilerTraceTestItem inputTestItem) throws Exception {
        inputTestItem.setupConfiguration();

        String threadName = "testThread" + System.nanoTime();

        Map<String, HoopoeProfiledInvocation> capturedData =
                HoopoeTestExecutor.<ProfilerTraceTestItem>create()
                        .withPackage("hoopoe.test.core.guineapigs")
                        .withPackage("hoopoe.test.supplements")
                        .withContext(testClassLoader -> {
                            Class instrumentedClass = testClassLoader.loadClass(inputTestItem.getEntryPointClass().getCanonicalName());
                            inputTestItem.setInstrumentedClass(instrumentedClass);
                            inputTestItem.prepareTest();
                            return inputTestItem;
                        })
                        .executeWithAgentLoaded(ProfilerTraceTestItem::executeTest, threadName)
                        .getCapturedData();

        // all captured executions in this thread are preparations and should be ignored during assertion
        capturedData.remove(Thread.currentThread().getName());
        inputTestItem.assertCapturedData(threadName, capturedData);
    }

}