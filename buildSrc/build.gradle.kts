import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    "compile"("pl.allegro.tech.build:axion-release-plugin:1.8.1")
    "compile"("com.moowork.gradle:gradle-node-plugin:1.2.0")
}