import hoopoe.gradle.buildscript.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import pl.allegro.tech.build.axion.release.domain.ChecksConfig
import pl.allegro.tech.build.axion.release.domain.TagNameSerializationConfig
import pl.allegro.tech.build.axion.release.domain.VersionConfig
import pl.allegro.tech.build.axion.release.domain.hooks.HookContext
import pl.allegro.tech.build.axion.release.domain.hooks.HooksConfig
import pl.allegro.tech.build.axion.release.domain.hooks.ReleaseHookAction
import pl.allegro.tech.build.axion.release.domain.properties.TagProperties
import pl.allegro.tech.build.axion.release.domain.scm.ScmPosition

buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("org.zeroturnaround:gradle-jrebel-plugin:1.1.4")
        classpath("pl.allegro.tech.build:axion-release-plugin:1.4.1")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.13.0")
    }
}

apply {
    plugin("pl.allegro.tech.build.axion-release")
    plugin("com.github.ben-manes.versions")
}

configure<VersionConfig> {
    tag(closureOf<TagNameSerializationConfig> {
        prefix = "hoopoe-profiler"
        initialVersion = KotlinClosure2<TagProperties, ScmPosition, String>({ _, _ ->
            "0.1.0-alpha1"
        })
    })
    versionIncrementer("incrementPrerelease")
    hooks(closureOf<HooksConfig> {
        pre(delegateClosureOf<HookContext> {
            project.ext.set("releaseVersion", version)
            println("Releasing $version")
        })
    })
    checks(closureOf<ChecksConfig> {
        uncommittedChanges = true
        aheadOfRemote = false
    })
}

val scmVersion: VersionConfig by project.extensions
val releasing = gradle.startParameter.taskNames.contains("release")

allprojects {
    group = "hoopoe-profiler"
    version = scmVersion.version

    repositories {
        mavenCentral()
        mavenLocal()
    }

    configurations {
        "hoopoe" {}
    }
}

subprojects {
    plugins.withType(JavaPlugin::class.java) {
        plugins.apply(JacocoPlugin::class.java)
    }

    afterEvaluate({
        tasks {
            withType(Test::class.java) {
                testLogging {
                    showStandardStreams = true
                    events("passed", "skipped", "failed", "standardOut")
                    showExceptions = true
                    showCauses = true
                    showStackTraces = true
                    exceptionFormat = TestExceptionFormat.FULL
                }

                afterSuite(printTestResult)
            }

            withType(JacocoReport::class.java) {
                reports.xml.isEnabled = true
                reports.html.isEnabled = false
                tasks.findByName("check")?.dependsOn(this)
            }

            withType(JavaCompile::class.java) {
                sourceCompatibility = "1.8"
            }
        }
    })
}
