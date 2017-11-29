package hoopoe.gradle.buildscript

import org.gradle.api.Project

object Versions {
    val bintrayPlugin = "1.8.0"
    val byteBuddy = "1.7.9"
    val commonIo = "2.6"
    val commonsLang3 = "3.7"
    val commonsText = "1.1"
    val fastClasspathScanner = "2.9.3"
    val gradleVersionsPlugin = "0.17.0"
    val guava = "23.5-jre"
    val hamcrest = "1.3"
    val httpClient = "4.5.3"
    val jackson = "2.9.2"
    val jetty = "9.4.7.RC0"
    val jrebelPlugin = "1.1.8"
    val junit = "4.12"
    val junitDataProvider = "1.13.1"
    val kotlin = "1.2.0"
    val logback = "1.2.3"
    val lombok = "1.16.18"
    val mockito = "2.12.0"
    val nodePlugin = "1.2.0"
    val slf4j = "1.7.25"
    val snakeYaml = "1.19"
    val springBoot = "2.0.0.M5"
    val testContainers = "1.4.3"
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
    val commonsLang3 = "org.apache.commons:commons-lang3:${Versions.commonsLang3}"
    val byteBuddy = "net.bytebuddy:byte-buddy:${Versions.byteBuddy}"
    val byteBuddyAgent = "net.bytebuddy:byte-buddy-agent:${Versions.byteBuddy}"
    val guava = "com.google.guava:guava:${Versions.guava}"
    val commonsText = "org.apache.commons:commons-text:${Versions.commonsText}"
    val fastClasspathScanner = "io.github.lukehutch:fast-classpath-scanner:${Versions.fastClasspathScanner}"
}

object Plugins {
    val jrebel = "org.zeroturnaround:gradle-jrebel-plugin:${Versions.jrebelPlugin}"
    val gradleVersions = "com.github.ben-manes:gradle-versions-plugin:${Versions.gradleVersionsPlugin}"
    val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintrayPlugin}"
    val springBoot = "org.springframework.boot:spring-boot-gradle-plugin:${Versions.springBoot}"
    val node = "com.moowork.gradle:gradle-node-plugin:${Versions.nodePlugin}"
}

val Project.libraries: Libraries
    get() = Libraries

val Project.librariesVersions: Versions
    get() = Versions