import hoopoe.gradle.buildscript.libraries
import hoopoe.gradle.buildscript.librariesVersions

apply {
    plugin("kotlin")
}

dependencies {
    "compile"(kotlin("stdlib-jdk8", librariesVersions.kotlin))
    "compile"(project(":hoopoe-api"))
}