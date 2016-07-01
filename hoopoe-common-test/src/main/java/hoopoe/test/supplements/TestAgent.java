package hoopoe.test.supplements;

import com.ea.agentloader.AgentLoader;
import hoopoe.core.HoopoeProfilerImpl;
import java.lang.instrument.Instrumentation;

// todo can we now replace it with real agent?
public class TestAgent {

    private static HoopoeProfilerImpl profiler;

    public static void load(String args) {
        AgentLoader.loadAgentClass(TestAgent.class.getName(), args);
    }

    public static void agentmain(String args, Instrumentation instrumentation)
            throws NoSuchFieldException, IllegalAccessException {

        profiler = new HoopoeProfilerImpl(args, instrumentation);
    }

    public static void unload() {
        profiler.unload();
    }
}
