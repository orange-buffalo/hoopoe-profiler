package hoopoe.api;

public interface HoopoeConfigurator {

    HoopoePluginsProvider createPluginsProvider();

    HoopoeProfilerStorage createProfilerStorage();

}
