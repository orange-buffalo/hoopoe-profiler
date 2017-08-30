
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.util.concurrent.Callable

apply {
    plugin("java")
}

configure<JavaPluginConvention> {
    sourceSets.create("hoopoeExtension")
    sourceSets.create("hoopoeAgent")
}

dependencies {
    compile("junit:junit:4.12")
    compile("org.testcontainers:testcontainers:1.4.2")
    compile(project(":hoopoe-core", "hoopoe"))
    compile(project(":hoopoe-api"))

    "hoopoeExtensionCompileOnly"(project(":hoopoe-api"))
    "hoopoeExtensionCompile"("org.eclipse.jetty:jetty-server:9.4.0.v20161208")
    "hoopoeExtensionCompile"("com.fasterxml.jackson.core:jackson-databind:2.9.0")

    "hoopoeAgentCompile"(project(":hoopoe-classloader"))
}

val sourceSets: SourceSetContainer = properties["sourceSets"] as SourceSetContainer

tasks {
    "buildHoopoeExtension"(HoopoeAssemblyTask::class) {
        classesSourceSetName = "hoopoeExtension"
        libFiles = Callable {
            HoopoeAssemblyTask.resolveLibsByConfiguration(project, "hoopoeExtensionCompile")
        }
        extensionClassName = "hoopoe.tests.extension.IntegrationTestExtension"

        dependsOn("hoopoeExtensionClasses")

        doLast {
            project.copy {
                from(outputArchive)
                into(sourceSets["hoopoeAgent"].output.resourcesDir)
            }
        }
    }

    "buildTestAgent"(Jar::class) {
        destinationDir = sourceSets["main"].output.resourcesDir
        archiveName = "hoopoe-test-agent.jar"
        from(sourceSets["hoopoeAgent"].output.resourcesDir)
        from(sourceSets["hoopoeAgent"].output.classesDirs)
        from(configurations["hoopoeAgentCompile"].resolvedConfiguration.resolvedArtifacts.map { zipTree(it.file) })
        from(configurations.compile) {
            include("**/hoopoe-core.zip")
        }

        manifest {
            attributes(mapOf("Premain-Class" to "hoopoe.tests.HoopoeTestAgent"))
        }

        dependsOn("buildHoopoeExtension")
    }


    "classes" {
        dependsOn("buildTestAgent")
    }

    "testClasses" {
        dependsOn("buildTestAgent")
    }
}