package hoopoe.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.createTask
import java.util.concurrent.Callable

open class HoopoeDependencyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply("java")

        val sourceSets = project.properties["sourceSets"] as SourceSetContainer
        val mainSourceSet = sourceSets.findByName("main")!!

        val hoopoeCopyTask = project.createTask("copyHoopoeArtifacts", Copy::class) {
            from(Callable {
                project.configurations.getByName("compile").resolvedConfiguration.resolvedArtifacts
                        .filter { it.classifier == "hoopoe" }
                        .map { it.file }
            })
            into(mainSourceSet.output.resourcesDir)
        }

        val hoopoeUberTask = project.createTask("hoopoeUberTask", Copy::class) {
            from(Callable {
                project.configurations.getByName("runtime").resolvedConfiguration.resolvedArtifacts
                        .filter { it.classifier != "hoopoe" }
                        .map { project.zipTree(it.file) }
            })
            into(mainSourceSet.java.outputDir)
            include("**/*.class")
            includeEmptyDirs = false
        }

        val processResourcesTask = project.tasks.findByName("processResources")!!
        processResourcesTask.dependsOn.add(hoopoeCopyTask)
        processResourcesTask.dependsOn.add(hoopoeUberTask)
    }
}
