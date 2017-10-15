import hoopoe.gradle.plugin.HoopoeAssemblyTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.util.concurrent.Callable
import hoopoe.gradle.buildscript.*

apply {
    plugin("java")
}

sourceSets {
    "hoopoeExtension" {}
    "hoopoeAgent" {}
}

dependencies {
    "compile"(libraries.junit)
    "compile"(libraries.testContainers)
    "compile"(libraries.snakeYaml)
    "compile"(libraries.httpClient)
    "compile"(libraries.jacksonDatabind)
    "compile"(project(":hoopoe-core"))
    "compile"(project(":hoopoe-core", "hoopoe"))
    "compile"(project(":hoopoe-api"))
    "compileOnly"(libraries.lombok)
    "runtime"(libraries.logback)

    "hoopoeExtensionCompileOnly"(project(":hoopoe-api"))
    "hoopoeExtensionCompileOnly"(libraries.slf4j)
    "hoopoeExtensionCompileOnly"(libraries.lombok)
    "hoopoeExtensionCompile"(libraries.jettyServer)
    "hoopoeExtensionCompile"(libraries.jacksonDatabind)

    "hoopoeAgentCompile"(project(":hoopoe-classloader"))
    "hoopoeAgentCompileOnly"(libraries.lombok)

    "itestCompile"(libraries.hamcrest)
    "itestCompileOnly"(libraries.lombok)
}

tasks {
    task<HoopoeAssemblyTask>("buildHoopoeExtension") {
        classesDirs = Callable {
            HoopoeAssemblyTask.resolveClassesAndResourcesBySourceSet(project, "hoopoeExtension")
        }
        libFiles = Callable {
            HoopoeAssemblyTask.resolveLibsByConfiguration(project, "hoopoeExtensionCompile")
        }
        extensionClassName = Callable { "hoopoe.tests.extension.IntegrationTestExtension" }
        archiveName = Callable { "hoopoe-integration-testing.zip" }

        dependsOn("hoopoeExtensionClasses")

        doLast {
            project.copy {
                from(outputArchive)
                into(sourceSets["main"].output.resourcesDir)
            }
        }
    }

    task<Jar>("buildTestAgent") {
        destinationDir = sourceSets["main"].output.resourcesDir
        archiveName = "hoopoe-test-agent.jar"
        from(sourceSets["hoopoeAgent"].output.resourcesDir)
        from(sourceSets["hoopoeAgent"].output.classesDirs)
        from(configurations["hoopoeAgentCompile"].resolvedConfiguration.resolvedArtifacts.map { zipTree(it.file) })
        from(configurations["compile"]) {
            include("**/hoopoe-core.zip")
        }

        manifest {
            attributes(mapOf("Premain-Class" to "hoopoe.tests.HoopoeTestAgent"))
        }
    }

    "classes" {
        dependsOn("buildTestAgent", "buildHoopoeExtension")
    }

    "testClasses" {
        dependsOn("buildTestAgent", "buildHoopoeExtension")
    }
}