import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSet

class HoopoeDependencyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.plugins.apply('java')

        SourceSet mainSourceSet = project.sourceSets.getByName('main');

        def hoopoeCopyTask = project.task(type: Copy, 'copyHoopoeArtifacts') {

            from {
                project.configurations.compile.resolvedConfiguration.resolvedArtifacts.findAll({
                    it.classifier == 'hoopoe'
                })*.file
            }
            into mainSourceSet.output.resourcesDir

        }

        Task processResourcesTask = project.tasks.findByName('processResources')
        processResourcesTask.dependsOn.add(hoopoeCopyTask);

    }
}

