package hoopoe.api;

public interface HoopoeConfiguration {

    HoopoeProfilerStorage createProfilerStorage();

    HoopoePluginsProvider createPluginsProvider();

    HoopoeTracer createTracer();

    long getMinimumTrackedInvocationTimeInNs();

}
