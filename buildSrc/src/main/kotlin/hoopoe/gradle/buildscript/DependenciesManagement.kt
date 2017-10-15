package hoopoe.gradle.buildscript

import org.gradle.api.Project

object Versions {
    val lombok = "1.16.18"
    val commonIo = "2.5"
    val mockito = "2.8.9"
    val hamcrest = "1.3"
    val junit = "4.12"
    val junitDataProvider = "1.12.0"
    val slf4j = "1.7.22"
}

object Libraries {
    val lombok = "org.projectlombok:lombok:${Versions.lombok}"
    val commonsIo = "commons-io:commons-io:${Versions.commonIo}"
    val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    val hamcrest = "org.hamcrest:hamcrest-all:${Versions.hamcrest}"
    val junit = "junit:junit:${Versions.junit}"
    val junitDataProvider = "com.tngtech.java:junit-dataprovider:${Versions.junitDataProvider}"
    val slf4j = "org.slf4j:slf4j-api:${Versions.slf4j}"
}

val Project.libraries: Libraries
    get() = Libraries