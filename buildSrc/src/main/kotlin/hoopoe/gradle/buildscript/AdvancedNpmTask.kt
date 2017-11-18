package hoopoe.gradle.buildscript

import com.moowork.gradle.node.npm.NpmTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

/**
 * A workaround for NpmTask which does not indicate input/outputs and is executed on every build.
 * https://github.com/srs/gradle-node-plugin/issues/225
 */
open class AdvancedNpmTask : DefaultTask() {

    @get:OutputDirectories
    var output: FileCollection = project.files()

    @get:InputFiles
    var inputs: FileCollection = project.files()

    private var npmTask : NpmTask = NpmTask()

    fun npm(config: NpmTask.() -> Unit) {
        config.invoke(npmTask)
    }

    @TaskAction
    fun exec() {
        npmTask.exec()
    }
}