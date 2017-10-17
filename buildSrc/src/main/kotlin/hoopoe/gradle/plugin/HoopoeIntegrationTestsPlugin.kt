package hoopoe.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.the

open class HoopoeIntegrationTestsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val sourceSets = project.the<JavaPluginConvention>().sourceSets
        val mainSourceSet = sourceSets.findByName("main")!!
        val itest = sourceSets.create("itest") {
            compileClasspath += mainSourceSet.output
            runtimeClasspath += mainSourceSet.output
        }

        project.configurations.findByName("itestCompile")!!.extendsFrom(project.configurations.getByName("testCompile"))

        project.configurations.findByName("itestRuntime")!!.extendsFrom(project.configurations.getByName("testRuntime"))

        project.tasks.create("integration-tests", Test::class.java) {
            testClassesDirs = itest.output.classesDirs
            classpath = itest.runtimeClasspath
        }
    }
}