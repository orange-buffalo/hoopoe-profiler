package hoopoe.api;

import java.util.Collection;

public interface HoopoeProfilerExtensionsProvider extends HoopoeProfilerSupplement {

    Collection<HoopoeProfilerExtension> createExtensions();

}