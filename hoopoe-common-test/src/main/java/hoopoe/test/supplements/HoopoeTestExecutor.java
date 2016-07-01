package hoopoe.test.supplements;

import hoopoe.api.HoopoeProfiledInvocation;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Assert;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoopoeTestExecutor<C> {

    private TestClassLoader testClassLoader = new TestClassLoader();
    private TestContextProducer<C> testContextProducer;

    @Getter
    private Map<String, HoopoeProfiledInvocation> capturedData = new HashMap<>();

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

        doAnswer(invocation ->
                capturedData.put(
                        ((Thread) invocation.getArguments()[0]).getName(),
                        (HoopoeProfiledInvocation) invocation.getArguments()[1]))
                .when(TestConfiguration.getStorageMock())
                .addInvocation(any(), any());

        TestClassLoader runnableLoader = new TestClassLoader();
        runnableLoader.includeClass(TestCodeRunnable.class);

        try {
            TestAgent.load("hoopoe.configuration.class=" + TestConfiguration.class.getCanonicalName());

            C testContext = testContextProducer.getTestContext(testClassLoader);

            Class testCodeRunnableClass = runnableLoader.loadClass(TestCodeRunnable.class.getName());
            Constructor<?> constructor = testCodeRunnableClass.getDeclaredConstructor(TestCode.class, Object.class);
            Runnable testCodeRunnable = (Runnable) constructor.newInstance(testCode, testContext);

            if (dedicatedThreadName == null) {
                testCodeRunnable.run();
            }
            else {
                Thread thread = new Thread(() -> {
                    try {
                        testCodeRunnable.run();
                    }
                    catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }, dedicatedThreadName);
                thread.start();
                thread.join();
            }
        }
        finally {
            TestAgent.unload();
        }

        for (Map.Entry<String, HoopoeProfiledInvocation> capturedDataEntry : capturedData.entrySet()) {
            HoopoeProfiledInvocation profiledInvocation = capturedDataEntry.getValue();
            if (profiledInvocation.getClassName().equals(TestCodeRunnable.class.getName())) {
                int subInvocationsCount = profiledInvocation.getChildren().size();
                if (subInvocationsCount == 0) {
                    capturedDataEntry.setValue(null);
                }
                else if (subInvocationsCount == 1) {
                    capturedDataEntry.setValue(profiledInvocation.getChildren().get(0));
                }
                else {
                    Assert.fail("Do not know what to do with multiple calls from TestCodeRunnable. Looks like mystery.");
                }
            }
        }

        return this;
    }

    public HoopoeProfiledInvocation getSingleProfiledInvocation() {
        assertThat(capturedData.size(), equalTo(1));
        return capturedData.values().iterator().next();
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

    public static class TestCodeRunnable implements Runnable {

        private TestCode testCode;
        private Object testContext;

        public TestCodeRunnable(TestCode testCode, Object testContext) {
            this.testCode = testCode;
            this.testContext = testContext;
        }

        @Override
        public void run() {
            try {
                testCode.execute(testContext);
            }
            catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

    }

}
