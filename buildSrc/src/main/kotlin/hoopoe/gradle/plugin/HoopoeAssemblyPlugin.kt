package hoopoe.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.createTask
import java.util.concurrent.Callable

open class HoopoeAssemblyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply("java")

        val hoopoeAssembly = project.extensions.create("hoopoeAssembly", HoopoeAssemblyConfig::class.java)

        val assembleHoopoeZip = project.createTask("assembleHoopoeZip", HoopoeAssemblyTask::class) {
            extensionClassName = Callable { hoopoeAssembly.extensionClass }
            pluginClassName = Callable { hoopoeAssembly.pluginClass }
        }

        assembleHoopoeZip.dependsOn.add(project.tasks.findByName("jar"))
        val assembleTask = project.tasks.findByName("assemble")!!
        assembleTask.dependsOn.add(assembleHoopoeZip)

        val generateRebelTask = project.tasks.findByName("generateRebel")
        generateRebelTask?.let { rebel -> assembleTask.dependsOn.add(rebel); }

        project.artifacts.add("hoopoe", project.provider(assembleHoopoeZip.outputArchive)) {
            classifier = "hoopoe"
            builtBy(assembleHoopoeZip)
        }
    }
}

open class HoopoeAssemblyConfig(
        var extensionClass: String? = null,
        var pluginClass: String? = null)