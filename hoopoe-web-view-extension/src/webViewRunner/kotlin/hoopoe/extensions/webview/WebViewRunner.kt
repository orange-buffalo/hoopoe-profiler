package hoopoe.extensions.webview

import hoopoe.api.HoopoeProfiledResult
import hoopoe.api.HoopoeProfiler
import hoopoe.api.configuration.HoopoeConfiguration

fun main(args: Array<String>) {
    val hoopoeWebViewExtension = HoopoeWebViewExtension()
    hoopoeWebViewExtension.init(ProfilerMock())
}

class ProfilerMock : HoopoeProfiler {
    private var profiling: Boolean = false
    private var profiledResult = createEmptyResult()

    override fun startProfiling() {
        profiling = true
    }

    override fun stopProfiling(): HoopoeProfiledResult {
        profiling = false
        // multiple results here to quickly switch in runtime during development of UI
        profiledResult = createEmptyResult()
        profiledResult = createRandomizedResult()
        profiledResult = createSingleThreadResult()
        profiledResult = createRandomizedMinimalResult()
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