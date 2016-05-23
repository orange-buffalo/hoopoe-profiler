package hoopoe.core;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiler;
import java.util.Collection;
import java.util.Collections;

public class HoopoePluginProviderImpl implements HoopoePluginsProvider {

    @Override
    public Collection<HoopoePlugin> createPlugins() {
        return Collections.emptyList();
    }

    @Override
    public void setupProfiler(HoopoeProfiler profiler) {

    }

}
