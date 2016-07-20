package hoopoe.core;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerExtensionsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

public class HoopoeConfigurationImpl implements HoopoeConfiguration {

    private static final long TRIM_THRESHOLD_IN_NS = 1_000_000;

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return new HoopoeStorageImpl();
    }

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return new HoopoePluginProviderImpl();
    }

    @Override
    public HoopoeProfilerExtensionsProvider createProfilerExtensionProvider() {
        return new HoopoeProfilerExtensionsProviderImpl();
    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return TRIM_THRESHOLD_IN_NS;
    }

    @Override
    public Collection<String> getEnabledPlugins() {
        return Collections.singleton("hoopoe-sql-queries-plugin");
    }

    @Override
    public Collection<Pattern> getExcludedClassesPatterns() {
        return Collections.emptyList();
    }

}
