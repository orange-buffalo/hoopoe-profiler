package hoopoe.api;

import java.util.Collection;

public interface HoopoeConfiguration {

    HoopoeProfilerStorage createProfilerStorage();

    HoopoePluginsProvider createPluginsProvider();

    long getMinimumTrackedInvocationTimeInNs();

    Collection<String> getEnabledPlugins();
}
