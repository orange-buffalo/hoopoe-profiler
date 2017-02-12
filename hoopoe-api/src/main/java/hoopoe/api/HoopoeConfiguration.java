package hoopoe.api;

import java.util.Collection;
import java.util.regex.Pattern;

// todo rewrite
public interface HoopoeConfiguration {


    long getMinimumTrackedInvocationTimeInNs();

    Collection<String> getEnabledPlugins();

    // todo include/exclude should be merged to "matches" method

    Collection<Pattern> getIncludedClassesPatterns();

    Collection<Pattern> getExcludedClassesPatterns();
}
