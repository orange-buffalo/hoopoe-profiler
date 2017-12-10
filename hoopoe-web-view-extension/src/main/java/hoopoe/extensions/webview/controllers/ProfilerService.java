package hoopoe.extensions.webview.controllers;

import hoopoe.api.HoopoeProfiledResult;

public interface ProfilerService {

    boolean startProfiling();

    HoopoeProfiledResult stopProfiling();

    HoopoeProfiledResult getLastProfiledResult();

    boolean isProfiling();

    HoopoeProfiledResult calculateHotSpots(int hotSpotsCountPerRoot);

}
