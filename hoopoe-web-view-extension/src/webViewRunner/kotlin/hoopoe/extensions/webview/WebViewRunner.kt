package hoopoe.extensions.webview

import hoopoe.api.HoopoeProfiledResult
import hoopoe.api.HoopoeProfiler
import hoopoe.api.configuration.HoopoeConfiguration
import hoopoe.core.tracer.HotSpotCalculator

fun main(args: Array<String>) {
    val hoopoeWebViewExtension = HoopoeWebViewExtension()
    hoopoeWebViewExtension.init(ProfilerMock())
}

class ProfilerMock : HoopoeProfiler {
    private var profiling: Boolean = false
    private var profiledResult = createEmptyResult()
    
    override fun calculateHotSpots(hotSpotsCountPerRoot: Int): HoopoeProfiledResult? {
        return HotSpotCalculator().calculateHotSpots(lastProfiledResult, hotSpotsCountPerRoot)
    }

    override fun startProfiling() {
        profiling = true
    }

    override fun stopProfiling(): HoopoeProfiledResult {
        profiling = false
        // multiple results here to quickly switch in runtime during development of UI
        profiledResult = createEmptyResult()
        profiledResult = createSingleThreadResult()
        profiledResult = createRandomizedMinimalResult()
        profiledResult = createRandomizedResult()
        return profiledResult
    }

    override fun getLastProfiledResult(): HoopoeProfiledResult {
        return profiledResult
    }

    override fun isProfiling(): Boolean {
        return profiling
    }

    override fun getConfiguration(): HoopoeConfiguration {
        throw UnsupportedOperationException("not yet implemented")
    }

}