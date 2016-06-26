import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Zip

class HoopoeAssemblyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply('java')

        project.extensions.create('hoopoeAssembly', HoopoeAssemblyConfig.class)

        SourceSet mainSourceSet = project.sourceSets.getByName('main');
        def runtimeConfiguration = project.configurations.findByName('runtime')
        def providedConfiguration = project.configurations.findByName('provided')

        def hoopoeManifestTask = project.task('hoopoeManifest') << {
            def pluginClass = project.extensions.hoopoeAssembly.pluginClass
            def extensionClass = project.extensions.hoopoeAssembly.extensionClass

            if (pluginClass || extensionClass) {
                def manifestFile = new File(temporaryDir, "META-INF/hoopoe.properties")
                manifestFile.getParentFile().mkdirs()

                if (pluginClass) {
                    manifestFile.text = "plugin.className=${pluginClass}"
                } else if (extensionClass) {
                    manifestFile.text = "extension.className=${extensionClass}"
                }
                return manifestFile
            }
        }

        def hoopoeZipTask = project.task(type: Zip, dependsOn: hoopoeManifestTask, 'hoopoeZipTask') {

            archiveName("${project.name}.zip")

            classifier('hoopoe')

            from(mainSourceSet.output.classesDir) {
                into('classes')
            }

            from(mainSourceSet.output.resourcesDir) {
                into('classes')
            }

            from(hoopoeManifestTask.temporaryDir)

            from({
                def artifacts = runtimeConfiguration.resolvedConfiguration.resolvedArtifacts -
                        providedConfiguration.resolvedConfiguration.resolvedArtifacts
                artifacts*.file
            }) {
                into('lib')
            }
        }

        Task assembleTask = project.tasks.findByName('assemble')
        hoopoeZipTask.dependsOn.add(project.tasks.findByName('jar'))
        def generateRebelTask = project.tasks.findByName('generateRebel')
        if (generateRebelTask) {
            assembleTask.dependsOn.add(generateRebelTask);
        }

        project.artifacts.add('hoopoe', hoopoeZipTask)
    }

}

class HoopoeAssemblyConfig {
    String extensionClass
    String pluginClass
}