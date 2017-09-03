package hoopoe.test.supplements;

import com.ea.agentloader.AgentLoader;
import hoopoe.core.HoopoeBootstrapper;
import hoopoe.core.HoopoeProfilerImpl;
import java.lang.instrument.Instrumentation;
import lombok.Getter;

// todo can we now replace it with real agent?
public class TestAgent {

    @Getter
    private static HoopoeProfilerImpl profiler;

    public static void load(String args) {
        AgentLoader.loadAgentClass(TestAgent.class.getName(), args);
    }

    public static void agentmain(String args, Instrumentation instrumentation)
            throws NoSuchFieldException, IllegalAccessException {

        profiler = HoopoeBootstrapper.bootstrapHoopoe(args, instrumentation);
    }

    public static void unload() {
        profiler.unload();
    }
}
