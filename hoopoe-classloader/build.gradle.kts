import org.gradle.kotlin.dsl.*
import hoopoe.gradle.buildscript.*

apply {
    plugin("java")
}

dependencies {
    "compileOnly"(libraries.lombok)

    "testCompile"(libraries.junit)
    "testCompile"(libraries.junitDataProvider)
    "testCompile"(libraries.hamcrest)
    "testCompile"(libraries.mockito)
    "testCompile"(libraries.commonsIo)
}