package hoopoe.core;

import java.lang.instrument.Instrumentation;

public class HoopoeAgent {

    public static void agentmain(String args, Instrumentation instrumentation) {
        premain(args, instrumentation);
    }

    public static void premain(String args, Instrumentation instrumentation) {
        HoopoeProfiler.init(args, instrumentation);
    }

}
