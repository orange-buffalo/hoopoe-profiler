package hoopoe.test.core.supplements;

import com.ea.agentloader.AgentLoader;
import hoopoe.core.HoopoeProfilerImpl;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class HoopoeTestAgent {

    private static Instrumentation instrumentation;
    private static ClassFileTransformer classFileTransformer;

    public static void load() {
        AgentLoader.loadAgentClass(HoopoeTestAgent.class.getName(), null);
    }

    public static void agentmain(String args, Instrumentation instrumentation)
            throws NoSuchFieldException, IllegalAccessException {

        HoopoeProfilerImpl profiler = new HoopoeProfilerImpl(args, instrumentation);
        HoopoeTestAgent.instrumentation = instrumentation;

        Field classFileTransformerField = HoopoeProfilerImpl.class.getDeclaredField("classFileTransformer");
        classFileTransformerField.setAccessible(true);
        classFileTransformer = (ClassFileTransformer) classFileTransformerField.get(profiler);
    }

    public static void unload() {
        if (instrumentation != null && classFileTransformer != null) {
            instrumentation.removeTransformer(classFileTransformer);
        }

        classFileTransformer = null;
        instrumentation = null;
    }
}
