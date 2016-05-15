package hoopoe.api;

import java.util.Collection;

public interface HoopoePluginsProvider {

    Collection<HoopoePlugin> loadPlugins(HoopoeConfigurator configurator);

}
