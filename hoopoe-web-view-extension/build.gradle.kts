import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmTask
import org.gradle.kotlin.dsl.*
import hoopoe.gradle.plugin.*
import hoopoe.gradle.buildscript.*
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.cast

object LibrariesVersions {
    val jetty = "9.4.0.v20161208"
    val jsonRpc = "1.5.0"
    val portletApi = "2.0"
}

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(hoopoe.gradle.buildscript.Plugins.node)
    }
}

apply {
    plugin("hoopoe-assemble-plugin")
    plugin("kotlin")
    plugin("org.zeroturnaround.gradle.jrebel")
    plugin("com.moowork.node")
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
    "webViewRunnerCompile"(libraries.fastClasspathScanner)
    "webViewRunnerCompile"(project(":hoopoe-core"))
    "webViewRunnerRuntime"(project(":hoopoe-api"))
    "webViewRunnerRuntime"(libraries.logback)
}

configure<HoopoeAssemblyConfig> {
    extensionClass = "hoopoe.extensions.webview.HoopoeWebViewExtension"
}

configure<NodeExtension> {
    version = "8.9.0"
    download = true
    nodeModulesDir = file("${project.projectDir}/src/main/web")
}

tasks {
    task<JavaExec>("runWebViewExtension") {
        main = "hoopoe.extensions.webview.WebViewRunnerKt"
        classpath = sourceSets["webViewRunner"].runtimeClasspath

        val webViewRunnerArgs = properties["webViewRunnerArgs"] as String?
        webViewRunnerArgs?.let {
            jvmArgs = it.split(" ")
        }

        dependsOn("generateRebel")
    }

    task<NpmTask>("npm") {
        val npmArgs = properties["npmArgs"] as String?
        npmArgs?.let {
            setArgs(it.split(" "))
        }
    }

    task<AdvancedNpmTask>("buildWebResources") {
        inputs = project.files("${project.projectDir}/src/main/web")
        output = project.files("${project.buildDir}/resources/main/static")
        npm {
            setArgs(listOf("run", "build"))
        }
        dependsOn("npmInstall")
    }

    "processResources" {
        dependsOn("buildWebResources")
    }
}
