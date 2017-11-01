import org.gradle.kotlin.dsl.*
import hoopoe.gradle.plugin.*
import hoopoe.gradle.buildscript.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.cast

object LibrariesVersions {
    val jetty = "9.4.0.v20161208"
    val jsonRpc = "1.5.0"
    val portletApi = "2.0"
}

apply {
    plugin("hoopoe-assemble-plugin")
    plugin("kotlin")
    plugin("org.zeroturnaround.gradle.jrebel")
}

sourceSets {
    "webViewRunner" {
        compileClasspath += sourceSets["main"].output
        compileClasspath += sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }
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
    "compileOnly"(libraries.lombok)
    "compileOnly"(libraries.slf4j)

    "webViewRunnerCompile"(kotlin("stdlib-jre8", librariesVersions.kotlin))
    "webViewRunnerCompile"(libraries.slf4j)
    "webViewRunnerRuntime"(project(":hoopoe-api"))
    "webViewRunnerRuntime"(libraries.logback)
}

configure<HoopoeAssemblyConfig> {
    extensionClass = "hoopoe.extensions.webview.HoopoeWebViewExtension"
}

tasks {
    task<JavaExec>("runWebViewExtension") {
        main = "hoopoe.extensions.webview.WebViewRunnerKt"
        classpath = sourceSets["webViewRunner"].runtimeClasspath

        val webViewRunnerArgs = properties["webViewRunnerArgs"] as String?
        webViewRunnerArgs?.let {
            jvmArgs = webViewRunnerArgs.split(" ")
        }

        dependsOn("generateRebel")
    }
}
