package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hoopoe.test.core.supplements.HoopoeTestAgent;
import hoopoe.test.core.supplements.HoopoeTestClassLoader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import lombok.Setter;
import org.junit.Test;

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

        HoopoeTestAgent.load();

        try {
            String threadName = "testThread" + System.nanoTime();

            Class instrumentedClass = classLoader.loadClass(testItem.getEntryPointClass().getCanonicalName());
            testItem.setInstrumentedClass(instrumentedClass);
            testItem.prepareTest();

            AtomicReference<Exception> exceptionReference = new AtomicReference();
            Thread thread = new Thread(() -> {
                try {
                    testItem.executeTest();
                }
                catch (Exception e) {
                    exceptionReference.set(e);
                }
            }, threadName);
            thread.start();
            thread.join();

            Exception exception = exceptionReference.get();
            if (exception != null) {
                throw exception;
            }
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

    protected static abstract class ProfilerTestItem {
        private String description;

        @Setter
        protected Class instrumentedClass;

        public ProfilerTestItem(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

        protected abstract Class getEntryPointClass();

        public abstract void prepareTest() throws Exception;

        public abstract void executeTest() throws Exception;
    }

}
