package hoopoe.core;

import hoopoe.api.HoopoeProfiler;

public class HoopoePluginProviderImpl extends HoopoeClassloaderBasedProvider {

    private static final String PLUGIN_CLASS_PROPERTY = "plugin.className";

    private HoopoeProfiler profiler;

//    @Override
//    public Collection<HoopoePlugin> createPlugins() {
//        return profiler.getConfiguration().getEnabledPlugins().stream()
//                .map(enabledPluginName -> (HoopoePlugin) load(enabledPluginName))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void setupProfiler(HoopoeProfiler profiler) {
//        this.profiler = profiler;
//    }

    @Override
    protected String getClassNameProperty() {
        return PLUGIN_CLASS_PROPERTY;
    }
}
