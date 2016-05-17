package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.api.HoopoeTraceNode;
import hoopoe.test.core.supplements.HoopoeTestAgent;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import hoopoe.test.core.supplements.HoopoeTestConfiguration;
import hoopoe.test.core.supplements.ProfilerTestItem;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.junit.Test;
import org.mockito.Mockito;

public abstract class AbstractProfilerTest {

    @Test
    @UseDataProvider("dataForProfilingTest")
    public void testProfiling(ProfilerTestItem testItem) {
        HoopoeTestClassLoader classLoader;
        try {
            classLoader = new HoopoeTestClassLoader();
        }
        catch (NotFoundException | IOException | CannotCompileException e) {
            throw new IllegalStateException(e);
        }

        ConcurrentMap<String, HoopoeTraceNode> capturedData = new ConcurrentHashMap<>();
        Mockito.doAnswer(
                invocation -> {
                    Object[] arguments = invocation.getArguments();
                    Thread thread = (Thread) arguments[0];
                    HoopoeTraceNode node = (HoopoeTraceNode) arguments[1];
                    capturedData.putIfAbsent(thread.getName(), node);
                    return null;
                })
                .when(HoopoeTestConfiguration.createStorageMock())
                .consumeThreadTraceResults(Mockito.any(), Mockito.any());

        HoopoeTestAgent.load("hoopoe.configuration.class=" + HoopoeTestConfiguration.class.getCanonicalName());

        try {
            Class instrumentedClass = classLoader.loadClass(testItem.getEntryPointClass().getCanonicalName());
            testItem.setInstrumentedClass(instrumentedClass);
            testItem.prepareTest();

            String threadName = "testThread" + System.nanoTime();
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
            HoopoeTestAgent.unload();

            // all captured executions in this thread are preparations and should be ignored during assertion
            capturedData.remove(Thread.currentThread().getName());
            testItem.assertCapturedData(threadName, capturedData);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            HoopoeTestAgent.unload();
        }
    }

    protected static Object[][] transform(ProfilerTestItem... items) {
        Object[][] data = new Object[items.length][1];
        for (int i = 0; i < items.length; i++) {
            data[i][0] = items[i];
        }
        return data;
    }

}
