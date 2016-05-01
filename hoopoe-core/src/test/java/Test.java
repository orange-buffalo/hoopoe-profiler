import com.ea.agentloader.AgentLoader;
import hoopoe.core.HoopoeAgent;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;

public class Test {

    @Before
    public void before() {
        AgentLoader.loadAgentClass(HoopoeAgent.class.getName(), "");
    }

    @org.junit.Test
    public void test() {
        BlaBlaBla blaBlaBla = new BlaBlaBla();
        int test = blaBlaBla.test();

        Assert.assertThat(test, CoreMatchers.equalTo(5));
    }

}
