package hoopoe.extensions.webview

import hoopoe.api.HoopoeInvocationAttribute
import hoopoe.api.HoopoeProfiledInvocation
import hoopoe.api.HoopoeProfiledInvocationRoot
import hoopoe.api.HoopoeProfiledResult

fun createEmptyResult() = profiledResult { }

fun createSingleThreadResult() = profiledResult {
    root {
        threadName = "http-8080-1"
        invocation {
            methodSignature = "test"
            className = "hello1"
            attribute { HoopoeInvocationAttribute.noTimeContribution("test", "hello") }
            invocation {
                className = "hello"
                methodSignature = "test1"
            }

        }
    }
}

private fun profiledResult(builderConfig: InvocationsListBuilder.() -> Unit): HoopoeProfiledResult {
    val builder = InvocationsListBuilder()
    builder.builderConfig()
    return HoopoeProfiledResult(builder.roots)
}

private fun configureInvocation(invocationConfig: InvocationBuilder.() -> Unit): HoopoeProfiledInvocation {
    val builder = InvocationBuilder()
    builder.invocationConfig()
    return builder.build()
}

private class InvocationsListBuilder {
    val roots = ArrayList<HoopoeProfiledInvocationRoot>()

    fun root(rootConfig: InvocationRootBuilder.() -> Unit) {
        val builder = InvocationRootBuilder()
        builder.rootConfig()
        roots.add(builder.build())
    }

}

private class InvocationRootBuilder {
    var threadName = ""
    private lateinit var invocation: HoopoeProfiledInvocation

    fun invocation(invocationConfig: InvocationBuilder.() -> Unit) {
        invocation = configureInvocation(invocationConfig)
    }

    fun build() = HoopoeProfiledInvocationRoot(threadName, invocation)
}

private class InvocationBuilder {
    var totalTimeInNs = 0L
    var ownTimeInNs = 0L
    lateinit var className: String
    lateinit var methodSignature: String
    var invocationsCount = 1
    private val children = ArrayList<HoopoeProfiledInvocation>()
    private var attributes = ArrayList<HoopoeInvocationAttribute>()

    fun invocation(invocationConfig: InvocationBuilder.() -> Unit) = children.add(configureInvocation(invocationConfig))

    fun attribute(provider: () -> HoopoeInvocationAttribute) = attributes.add(provider())

    fun build() = HoopoeProfiledInvocation(
            className,
            methodSignature,
            children,
            totalTimeInNs,
            ownTimeInNs,
            invocationsCount,
            attributes)
}