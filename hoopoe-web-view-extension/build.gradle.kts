import org.gradle.kotlin.dsl.*
import hoopoe.gradle.plugin.*
import hoopoe.gradle.buildscript.*

object LibrariesVersions {
    val jetty = "9.4.0.v20161208"
    val jsonRpc = "1.5.0"
    val portletApi = "2.0"
}

apply {
    plugin("hoopoe-assemble-plugin")
}

dependencies {
    "compile"("org.eclipse.jetty:jetty-server:${LibrariesVersions.jetty}")
    "compile"("org.eclipse.jetty:jetty-servlet:${LibrariesVersions.jetty}")
    "compile"(libraries.commonsIo)
    "compile"("com.github.briandilley.jsonrpc4j:jsonrpc4j:${LibrariesVersions.jsonRpc}") {
        exclude(module = "slf4j-api")    // todo check why this dependency fails
    }

    "compileOnly"(project(":hoopoe-api"))
    "compileOnly"("javax.portlet:portlet-api:${LibrariesVersions.portletApi}")
}

configure<HoopoeAssemblyConfig> {
    extensionClass = "hoopoe.extensions.webview.HoopoeWebViewExtension"
}