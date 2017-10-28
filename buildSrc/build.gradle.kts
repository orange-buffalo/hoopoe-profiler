import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    "compile"("pl.allegro.tech.build:axion-release-plugin:1.8.1")
}