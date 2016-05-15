package hoopoe.core;

import hoopoe.api.HoopoeConfigurator;
import hoopoe.api.HoopoePluginsProvider;
import hoopoe.api.HoopoeProfilerStorage;

class DefaultConfigurator implements HoopoeConfigurator {

    @Override
    public HoopoePluginsProvider createPluginsProvider() {
        return null;
    }

    @Override
    public HoopoeProfilerStorage createProfilerStorage() {
        return null;
    }
}
