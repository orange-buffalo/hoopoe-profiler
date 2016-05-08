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




        def hoopoeZipTask = project.task(type: Zip, 'hoopoeZip') {

            archiveName("${project.name}.zip")

            classifier('hoopoe')

            from(mainSourceSet.output.classesDir) {
                into('classes')
            }

            from(mainSourceSet.output.resourcesDir) {
                into('classes')
            }

            from({
                runtimeConfiguration.resolvedConfiguration.resolvedArtifacts*.file
            }) {
                into('lib')
            }
        }


        Task assembleTask = project.tasks.findByName('assemble')
        hoopoeZipTask.dependsOn.add(project.tasks.findByName('jar'))
        assembleTask.dependsOn.add(hoopoeZipTask);

        project.artifacts.add('hoopoe', hoopoeZipTask)

    }


}

class HoopoeAssemblyConfig {
    String extensionClass
    String pluginClass
}
