package hoopoe.extensions.webview

val log = org.slf4j.LoggerFactory.getLogger("hoopoe.profiler")

fun main(args: Array<String>) {
    log.info("starting")
    val hoopoeWebViewExtension = HoopoeWebViewExtension()
    hoopoeWebViewExtension.init(null)
    log.info("started!")
}