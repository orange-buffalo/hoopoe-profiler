package hoopoe.core;

import hoopoe.api.HoopoePlugin;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfiler;
import java.util.Collection;
import java.util.stream.Collectors;

public class HoopoePluginProviderImpl extends HoopoeClassloaderBasedProvider implements HoopoePluginsProvider {

    private static final String PLUGIN_CLASS_PROPERTY = "plugin.className";

    private HoopoeProfiler profiler;

    @Override
    public Collection<HoopoePlugin> createPlugins() {
        return profiler.getConfiguration().getEnabledPlugins().stream()
                .map(enabledPluginName -> (HoopoePlugin) load(enabledPluginName))
                .collect(Collectors.toList());
    }

    @Override
    public void setupProfiler(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    protected String getClassNameProperty() {
        return PLUGIN_CLASS_PROPERTY;
    }
}
