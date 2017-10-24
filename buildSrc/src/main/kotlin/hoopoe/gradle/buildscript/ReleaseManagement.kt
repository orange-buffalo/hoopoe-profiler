package hoopoe.gradle.buildscript

import org.gradle.api.Project
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.closureOf
import pl.allegro.tech.build.axion.release.OutputCurrentVersionTask
import pl.allegro.tech.build.axion.release.ReleaseTask
import pl.allegro.tech.build.axion.release.VerifyReleaseTask
import pl.allegro.tech.build.axion.release.domain.ChecksConfig
import pl.allegro.tech.build.axion.release.domain.TagNameSerializationConfig
import pl.allegro.tech.build.axion.release.domain.VersionConfig
import pl.allegro.tech.build.axion.release.domain.hooks.HookContext
import pl.allegro.tech.build.axion.release.domain.hooks.HooksConfig
import pl.allegro.tech.build.axion.release.domain.properties.TagProperties
import pl.allegro.tech.build.axion.release.domain.scm.ScmPosition

val Project.releasing: Boolean
    get() = gradle.startParameter.taskNames.contains("tagRelease")

/**
 * We use only a subset of axion release plugin tasks, configuring them here to not add what we do not need and what
 * could be dangerous (like create extra tags with markNextVersion).
 *
 * @see http://axion-release-plugin.readthedocs.io/en/latest/configuration/tasks.html
 */
fun Project.prepareReleaseManagement() {

    extensions.create("scmVersion", VersionConfig::class.java, this).apply {

        tag(closureOf<TagNameSerializationConfig> {
            prefix = "hoopoe-profiler"
            initialVersion = KotlinClosure2<TagProperties, ScmPosition, String>({ _, _ ->
                "0.1.0-alpha1"
            })
        })

        versionIncrementer("incrementPrerelease")

        hooks(closureOf<HooksConfig> {
            pre(KotlinClosure1<HookContext, Unit>({
                println("Releasing $version")
            }))
        })
        checks(closureOf<ChecksConfig> {
            uncommittedChanges = true
            aheadOfRemote = false
        })

        localOnly = true
    }

    tasks.create("verifyReleaseTag", VerifyReleaseTask::class.java) {
        group = "Release"
        description = "Verifies code is ready to create a release tag."
    }

    val tagReleaseTask = tasks.create("tagRelease", ReleaseTask::class.java) {
        group = "Release"
        description = "Creates a release tag."

    }
    tagReleaseTask.dependsOn("verifyReleaseTag")

    tasks.create("currentVersion", OutputCurrentVersionTask::class.java) {
        group = "Help"
        description = "Prints current project version extracted from SCM."
    }

}
