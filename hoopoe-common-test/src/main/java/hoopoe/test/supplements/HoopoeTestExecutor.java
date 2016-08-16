package hoopoe.test.supplements;

import hoopoe.api.HoopoeProfiledInvocation;
import hoopoe.api.HoopoeProfiledResult;
import hoopoe.core.HoopoeProfilerImpl;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoopoeTestExecutor<C> {

    private TestClassLoader testClassLoader = new TestClassLoader();
    private TestContextProducer<C> testContextProducer;

    @Getter
    private HoopoeProfiledResult profiledResult;

    public static <C> HoopoeTestExecutor<C> create() {
        return new HoopoeTestExecutor<>();
    }

    public static HoopoeTestExecutor<ClassInstanceContext> forClassInstance(String className) throws Exception {
        return HoopoeTestExecutor.<ClassInstanceContext>create()
                .withContext(testClassLoader ->
                        new HoopoeTestExecutor.ClassInstanceContext(testClassLoader.loadClass(className)));
    }

    public HoopoeTestExecutor<C> withPackage(String packageName) {
        testClassLoader.includePackages(packageName);
        return this;
    }

    public HoopoeTestExecutor<C> withContext(TestContextProducer<C> producer) throws Exception {
        testContextProducer = producer;
        return this;
    }

    public HoopoeTestExecutor<C> executeWithAgentLoaded(TestCode<C> testCode) throws Exception {
        return executeWithAgentLoaded(testCode, null);
    }

    public HoopoeTestExecutor<C> executeWithAgentLoaded(TestCode<C> testCode, String dedicatedThreadName) throws Exception {
        try {
            TestAgent.load("hoopoe.configuration.class=" + TestConfiguration.class.getCanonicalName());

            C testContext = testContextProducer.getTestContext(testClassLoader);

            HoopoeProfilerImpl profiler = TestAgent.getProfiler();
            profiler.startProfiling();

            if (dedicatedThreadName == null) {
                testCode.execute(testContext);
            }
            else {
                Thread thread = new Thread(() -> {
                    try {
                        testCode.execute(testContext);
                    }
                    catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }, dedicatedThreadName);
                thread.start();
                thread.join();
            }

            profiledResult = profiler.stopProfiling();
        }
        finally {
            TestAgent.unload();
        }

        return this;
    }

    public HoopoeProfiledInvocation getSingleProfiledInvocation() {
        assertThat("There is nothing profiled", profiledResult, notNullValue());
        assertThat("Expected single invocation profiled", profiledResult.getInvocations().size(), equalTo(1));
        return profiledResult.getInvocations().iterator().next().getInvocation();
    }

    public Map<String, HoopoeProfiledInvocation> toThreadInvocationMap() {
        Map<String, HoopoeProfiledInvocation> threadMap = new HashMap<>();
        if (profiledResult != null) {
            profiledResult.getInvocations().forEach(invocationRoot -> {
                String threadName = invocationRoot.getThreadName();
                assertThat("More than one invocation for thread " + threadName + " profiled",
                        !threadMap.containsKey(threadName));
                threadMap.put(threadName, invocationRoot.getInvocation());
            });
        }
        return threadMap;
    }

    public interface TestContextProducer<C> {
        C getTestContext(TestClassLoader testClassLoader) throws Exception;
    }

    public interface TestCode<C> {
        void execute(C context) throws Exception;
    }

    @Getter
    public static class ClassInstanceContext {
        private Class clazz;
        private Object instance;

        public ClassInstanceContext(Class clazz) throws IllegalAccessException, InstantiationException {
            this(clazz, clazz.newInstance());
        }

        public ClassInstanceContext(Class clazz, Object instance) {
            this.clazz = clazz;
            this.instance = instance;
        }
    }

}
