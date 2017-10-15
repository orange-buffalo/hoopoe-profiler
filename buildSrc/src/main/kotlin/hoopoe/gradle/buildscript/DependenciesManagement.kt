package hoopoe.gradle.buildscript

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the

object Versions {
    val lombok = "1.16.18"
    val commonIo = "2.5"
    val mockito = "2.8.9"
    val hamcrest = "1.3"
    val junit = "4.12"
    val junitDataProvider = "1.12.0"
    val slf4j = "1.7.22"
    val testContainers = "1.4.2"
    val snakeYaml = "1.18"
    val httpClient = "4.5.3"
    val jackson = "2.9.0"
    val logback = "1.2.3"
    val jetty = "9.4.0.v20161208"
    val commonsLang3 = "3.5"
    val byteBuddy = "1.7.6"
    val guava = "21.0"
}

object Libraries {
    val lombok = "org.projectlombok:lombok:${Versions.lombok}"
    val commonsIo = "commons-io:commons-io:${Versions.commonIo}"
    val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    val hamcrest = "org.hamcrest:hamcrest-all:${Versions.hamcrest}"
    val junit = "junit:junit:${Versions.junit}"
    val junitDataProvider = "com.tngtech.java:junit-dataprovider:${Versions.junitDataProvider}"
    val slf4j = "org.slf4j:slf4j-api:${Versions.slf4j}"
    val testContainers = "org.testcontainers:testcontainers:${Versions.testContainers}"
    val snakeYaml = "org.yaml:snakeyaml:${Versions.snakeYaml}"
    val httpClient = "org.apache.httpcomponents:httpclient:${Versions.httpClient}"
    val jacksonDatabind = "com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}"
    val logback = "ch.qos.logback:logback-classic:${Versions.logback}"
    val jettyServer = "org.eclipse.jetty:jetty-server:${Versions.jetty}"
    val commonsLang3= "org.apache.commons:commons-lang3:${Versions.commonsLang3}"
    val byteBuddy = "net.bytebuddy:byte-buddy:${Versions.byteBuddy}"
    val byteBuddyAgent = "net.bytebuddy:byte-buddy-agent:${Versions.byteBuddy}"
    val guava = "com.google.guava:guava:${Versions.guava}"
}

val Project.libraries: Libraries
    get() = Libraries

val Project.sourceSets: SourceSetContainer
    get() = the<JavaPluginConvention>().sourceSets