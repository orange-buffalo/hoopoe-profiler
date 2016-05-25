package hoopoe.api;

import java.util.Collection;

public interface HoopoeConfiguration {

    HoopoeProfilerStorage createProfilerStorage();

    HoopoePluginsProvider createPluginsProvider();

    HoopoeTracer createTracer();

    long getMinimumTrackedInvocationTimeInNs();

    Collection<String> getEnabledPlugins();
}
