package hoopoe.core;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;
import hoopoe.api.HoopoeTracer;
import java.util.Collection;
import java.util.Collections;

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
    public HoopoeTracer createTracer() {
        return new HoopoeTracerImpl();
    }

    @Override
    public long getMinimumTrackedInvocationTimeInNs() {
        return TRIM_THRESHOLD_IN_NS;
    }

    @Override
    public Collection<String> getEnabledPlugins() {
        return Collections.singleton("sql-queries-plugin");
    }

}
