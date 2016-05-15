package hoopoe.test.core;

import hoopoe.test.core.guineapigs.BlaBlaBla;
import org.junit.Test;

public class HoopoeProfilerTest extends AbstractHoopoeProfilerTest {

    @Test
    public void test() throws InterruptedException, NoSuchFieldException, IllegalAccessException {

        executeProfiling(BlaBlaBla.class, "test");


        //System.out.println(System.getProperty("java.class.path"));


        // Assert.assertThat(test, CoreMatchers.equalTo(5));
    }

}
