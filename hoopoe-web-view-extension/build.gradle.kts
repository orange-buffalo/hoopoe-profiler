import com.google.protobuf.gradle.ExecutableLocator
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufConfigurator
import com.google.protobuf.gradle.ProtobufConvention
import org.gradle.kotlin.dsl.*
import hoopoe.gradle.plugin.*
import hoopoe.gradle.buildscript.*
import org.gradle.api.tasks.JavaExec
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.cast

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(hoopoe.gradle.buildscript.Plugins.protobuf)
    }
}

object LibrariesVersions {
    val jetty = "9.4.0.v20161208"
    val jsonRpc = "1.5.0"
    val portletApi = "2.0"
}

apply {
    plugin("hoopoe-assemble-plugin")
    plugin("kotlin")
    plugin("org.zeroturnaround.gradle.jrebel")
    plugin("com.google.protobuf")
}

val protobufGeneratedDir = "$buildDir/generated-sources/protobuf"

sourceSets {
    "webViewRunner" {
        compileClasspath += sourceSets["main"].output
        compileClasspath += sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }
    "main" {
        java {
            setSrcDirs(files("src/main/java", "${protobufGeneratedDir}/main/java", "${protobufGeneratedDir}/main/grpc"))
        }
    }
}

dependencies {
    "compile"("org.eclipse.jetty:jetty-server:${LibrariesVersions.jetty}")
    "compile"("org.eclipse.jetty:jetty-servlet:${LibrariesVersions.jetty}")
    "compile"(libraries.commonsIo)
    "compile"("com.github.briandilley.jsonrpc4j:jsonrpc4j:${LibrariesVersions.jsonRpc}") {
        exclude(module = "slf4j-api")    // todo check why this dependency fails
    }
    "compile"(libraries.protobuf)
    "compile"(libraries.grpcStub)
    "compile"(libraries.grpcProtobuf)

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

the<ProtobufConvention>().protobuf(
        closureOf<ProtobufConfigurator> {
            protoc(
                    closureOf<ExecutableLocator> {
                        artifact = "com.google.protobuf:protoc:${Versions.protobuf}"
                    })
            generatedFilesBaseDir = protobufGeneratedDir

            plugins(closureOf<NamedDomainObjectContainer<ExecutableLocator>> {
                create("grpc") {
                    artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}"
                }
            })

            generateProtoTasks(closureOf<ProtobufConfigurator.GenerateProtoTaskCollection> {
                for (generateProtoTask in all()) {
                    generateProtoTask.plugins(closureOf<NamedDomainObjectContainer<GenerateProtoTask.PluginOptions>> {
                        create("grpc")
                    })
                }
            })
        })

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
}
