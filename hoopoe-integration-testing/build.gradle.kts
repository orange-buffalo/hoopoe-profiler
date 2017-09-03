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
    compile("org.yaml:snakeyaml:1.18")
    compile("org.apache.httpcomponents:httpclient:4.5.3")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.0")
    compile(project(":hoopoe-core", "hoopoe"))
    compile(project(":hoopoe-api"))
    "compileOnly"("org.projectlombok:lombok-maven:1.16.18.1")
    "runtime"("ch.qos.logback:logback-classic:1.2.3")

    "hoopoeExtensionCompileOnly"(project(":hoopoe-api"))
    "hoopoeExtensionCompileOnly"("org.slf4j:slf4j-api:1.7.22")
    "hoopoeExtensionCompile"("org.eclipse.jetty:jetty-server:9.4.0.v20161208")
    "hoopoeExtensionCompile"("com.fasterxml.jackson.core:jackson-databind:2.9.0")

    "hoopoeAgentCompile"(project(":hoopoe-classloader"))

    "itestCompile"("org.hamcrest:hamcrest-all:1.3")
}

val sourceSets: SourceSetContainer = properties["sourceSets"] as SourceSetContainer

tasks {
    "buildHoopoeExtension"(HoopoeAssemblyTask::class) {
        classesDirs = Callable {
            HoopoeAssemblyTask.resolveClassesAndResourcesBySourceSet(project, "hoopoeExtension")
        }
        libFiles = Callable {
            HoopoeAssemblyTask.resolveLibsByConfiguration(project, "hoopoeExtensionCompile")
        }
        extensionClassName = Callable { "hoopoe.tests.extension.IntegrationTestExtension" }
        attachToArtifacts = Callable { false }
        archiveName = Callable { "hoopoe-integration-testing.zip" }

        dependsOn("hoopoeExtensionClasses")

        doLast {
            project.copy {
                from(outputArchive)
                into(sourceSets["main"].output.resourcesDir)
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
    }


    "classes" {
        dependsOn("buildTestAgent", "buildHoopoeExtension")
    }

    "testClasses" {
        dependsOn("buildTestAgent", "buildHoopoeExtension")
    }
}