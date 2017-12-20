import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayUploadTask
import hoopoe.gradle.buildscript.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.scan.config.BuildScanConfig
import org.gradle.kotlin.dsl.`build-scan`
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.zeroturnaround.jrebel.gradle.RebelGenerateTask
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
        classpath(hoopoe.gradle.buildscript.Plugins.jrebel)
        classpath(hoopoe.gradle.buildscript.Plugins.gradleVersions)
        classpath(hoopoe.gradle.buildscript.Plugins.bintray)
        classpath(kotlin("gradle-plugin", hoopoe.gradle.buildscript.Versions.kotlin))
        classpath(hoopoe.gradle.buildscript.Plugins.sonar)
    }
}

plugins {
    `build-scan`
}

apply {
    plugin("com.github.ben-manes.versions")
    plugin("org.sonarqube")
}

prepareReleaseManagement()
val scmVersion: VersionConfig by project.extensions

buildScan {
    setLicenseAgreementUrl("https://gradle.com/terms-of-service")
    setLicenseAgree("yes")
    publishAlwaysIf(System.getenv("CI") == "true")
}

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

            withType(RebelGenerateTask::class.java) {
                onlyIf { !releasing }
            }

            withType(BintrayUploadTask::class.java) {
                the<BintrayExtension>().apply {
                    user = System.getenv("BINTRAY_USER")
                    key = System.getenv("BINTRAY_KEY")
                    pkg(closureOf<BintrayExtension.PackageConfig> {
                        repo = "hoopoe-profiler"
                        version(closureOf<BintrayExtension.VersionConfig> {
                            name = "${project.version}"
                            vcsTag = "hoopoe-profiler-${project.version}"
                            released = java.util.Date().toString()
                        })
                    })
                }
                dependsOn("build")
            }

            withType(KotlinCompile::class.java) {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
    })
}

tasks {
    task<Wrapper>("wrapper") {
        distributionUrl = "https://services.gradle.org/distributions/gradle-4.4-all.zip"
    }
}