package hoopoe.core;

import hoopoe.api.HoopoeConfiguration;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;

public class HoopoeConfigurationImpl implements HoopoeConfiguration {

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return new HoopoeStorageImpl();
    }

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return new HoopoePluginProviderImpl();
    }

}
