package hoopoe.api;

public interface HoopoeProfiler {

    HoopoeConfiguration getConfiguration();

    void startProfiling();

    HoopoeProfiledResult stopProfiling();

}
