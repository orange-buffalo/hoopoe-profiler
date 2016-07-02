package hoopoe.api;

import java.util.Collection;
import java.util.regex.Pattern;

public interface HoopoeConfiguration {

    HoopoeProfilerStorage createProfilerStorage();

    HoopoePluginsProvider createPluginsProvider();

    HoopoeProfilerExtensionsProvider createProfilerExtensionProvider();

    long getMinimumTrackedInvocationTimeInNs();

    Collection<String> getEnabledPlugins();

    Collection<Pattern> getExcludedClassesPatterns();
}
