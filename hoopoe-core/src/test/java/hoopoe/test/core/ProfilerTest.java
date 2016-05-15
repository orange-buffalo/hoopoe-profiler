package hoopoe.test.core;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import hoopoe.test.core.guineapigs.BlaBlaBla;
import java.lang.reflect.Method;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ProfilerTest extends AbstractProfilerTest {

    @DataProvider
    public static Object[][] dataForProfilingTest() {
        return transform(
                new ProfilerTestItem("Simple test") {
                    Object object;
                    Method method;

                    @Override
                    protected Class getEntryPointClass() {
                        return BlaBlaBla.class;
                    }

                    @Override
                    public void prepareTest() throws Exception {
                        method = instrumentedClass.getMethod("test");
                        object = instrumentedClass.newInstance();
                    }

                    @Override
                    public void executeTest() throws Exception {
                        method.invoke(object);
                    }
                },

                new ProfilerTestItem("Simple test") {
                    Object object;
                    Method method;

                    @Override
                    protected Class getEntryPointClass() {
                        return BlaBlaBla.class;
                    }

                    @Override
                    public void prepareTest() throws Exception {
                        method = instrumentedClass.getMethod("tt");
                        object = instrumentedClass.newInstance();
                    }

                    @Override
                    public void executeTest() throws Exception {
                        method.invoke(object);
                    }
                }
        );
    }

}
