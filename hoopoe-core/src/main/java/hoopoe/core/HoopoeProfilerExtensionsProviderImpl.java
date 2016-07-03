package hoopoe.core;

import hoopoe.api.HoopoeProfiler;
import hoopoe.api.HoopoeProfilerExtension;
import hoopoe.api.HoopoeProfilerExtensionsProvider;
import java.util.Collection;
import java.util.Collections;

public class HoopoeProfilerExtensionsProviderImpl
        extends HoopoeClassloaderBasedProvider
        implements HoopoeProfilerExtensionsProvider {

    private HoopoeProfiler profiler;

    @Override
    public Collection<HoopoeProfilerExtension> createExtensions() {
        HoopoeProfilerExtension extension = (HoopoeProfilerExtension) load("hoopoe-web-view-extension");
        extension.setupProfiler(profiler);
        return Collections.singleton(extension);
    }

    // todo common code should be reusable
    @Override
    public void setupProfiler(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    protected String getClassNameProperty() {
        return "extension.className";
    }

}
