package hoopoe.extensions.webview.controllers;

import hoopoe.api.HoopoeProfiledResult;
import hoopoe.api.HoopoeProfiler;

public class ProfilerServiceImpl implements ProfilerService {

    private HoopoeProfiler profiler;

    public ProfilerServiceImpl(HoopoeProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    public boolean startProfiling() {
        profiler.startProfiling();
        return true;
    }

    @Override
    public HoopoeProfiledResult stopProfiling() {
        return profiler.stopProfiling();
    }

    @Override
    public HoopoeProfiledResult getLastProfiledResult() {
        return profiler.getLastProfiledResult();
    }

    @Override
    public boolean isProfiling() {
        return profiler.isProfiling();
    }

}
