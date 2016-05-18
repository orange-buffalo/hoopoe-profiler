package hoopoe.api;

import java.util.Collection;

public interface HoopoePluginsProvider extends HoopoeProfilerSupplement {

    Collection<HoopoePlugin> createPlugins();

}
