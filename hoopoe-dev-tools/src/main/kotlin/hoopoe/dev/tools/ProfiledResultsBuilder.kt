package hoopoe.dev.tools

import hoopoe.api.HoopoeInvocationAttribute
import hoopoe.api.HoopoeProfiledInvocation
import hoopoe.api.HoopoeProfiledInvocationRoot
import hoopoe.api.HoopoeProfiledResult

fun profiledResult(builderConfig: InvocationsListBuilder.() -> Unit): HoopoeProfiledResult {
    val builder = InvocationsListBuilder()
    builder.builderConfig()
    return HoopoeProfiledResult(builder.roots)
}

fun profiledInvocationRoot(rootConfig: InvocationRootBuilder.() -> Unit): HoopoeProfiledInvocationRoot {
    val builder = InvocationRootBuilder()
    builder.rootConfig()
    return builder.build()
}

fun configureInvocation(invocationConfig: InvocationBuilder.() -> Unit): HoopoeProfiledInvocation {
    val builder = InvocationBuilder()
    builder.invocationConfig()
    return builder.build()
}

class InvocationsListBuilder {
    val roots = ArrayList<HoopoeProfiledInvocationRoot>()

    fun root(rootConfig: InvocationRootBuilder.() -> Unit) {
        roots.add(profiledInvocationRoot(rootConfig))
    }

}

class InvocationRootBuilder {
    var threadName = ""
    lateinit var invocation: HoopoeProfiledInvocation

    fun invocation(invocationConfig: InvocationBuilder.() -> Unit) {
        invocation = configureInvocation(invocationConfig)
    }

    fun build() = HoopoeProfiledInvocationRoot(threadName, invocation)
}

class InvocationBuilder {
    var totalTimeInNs = 0L
    var ownTimeInNs = 0L
    lateinit var className: String
    lateinit var methodSignature: String
    var invocationsCount = 1
    private val children = ArrayList<HoopoeProfiledInvocation>()
    private var attributes = ArrayList<HoopoeInvocationAttribute>()

    fun invocation(invocationConfig: InvocationBuilder.() -> Unit) = children.add(configureInvocation(invocationConfig))

    fun addChild(child: HoopoeProfiledInvocation) {
        children.add(child)
    }

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