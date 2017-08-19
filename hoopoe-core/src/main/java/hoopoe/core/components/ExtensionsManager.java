package hoopoe.core.components;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.extensions.HoopoeProfilerExtension;
import hoopoe.core.configuration.Configuration;
import hoopoe.core.configuration.EnabledComponentData;

public class ExtensionsManager {

    private final Configuration configuration;
    private final ComponentLoader componentLoader;

    public ExtensionsManager(
            Configuration configuration,
            ComponentLoader componentLoader) {

        this.configuration = configuration;
        this.componentLoader = componentLoader;
    }

    public void initExtensions(HoopoeProfiler profiler) {
        for (EnabledComponentData enabledExtension : configuration.getEnabledExtensions()) {
            HoopoeProfilerExtension extension = componentLoader.loadComponent(
                    enabledExtension.getBinariesPath(), HoopoeProfilerExtension.class);
            configuration.setExtensionConfiguration(extension, enabledExtension.getId());
            extension.init(profiler);
        }
    }

}
