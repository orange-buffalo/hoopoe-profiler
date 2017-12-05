import com.sun.javafx.scene.CameraHelper.project
import hoopoe.gradle.buildscript.libraries
import hoopoe.gradle.buildscript.librariesVersions
import hoopoe.gradle.buildscript.sourceSets
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.the

apply {
    plugin("hoopoe-assemble-plugin")
    plugin("kotlin")
}

dependencies {
    "compile"(project(":hoopoe-classloader"))
    "compile"(libraries.slf4j)
    "compile"(libraries.commonsLang3)
    "compile"(libraries.commonsText)
    "compile"(libraries.commonsIo)
    "compile"(libraries.byteBuddy)
    "compile"(libraries.guava)
    "compile"(libraries.snakeYaml)
    "compile"(project(":hoopoe-api"))

    "compileOnly"(libraries.lombok)

    "runtime"(libraries.logback)

    "testCompile"(libraries.junit)
    "testCompile"(libraries.junitDataProvider)
    "testCompile"(libraries.hamcrest)
    "testCompile"(libraries.mockito)
    "testCompile"(libraries.byteBuddyAgent)
    "testCompile"(kotlin("stdlib-jdk8", librariesVersions.kotlin))
    "testCompile"(project(":hoopoe-dev-tools"))

    "testCompileOnly"(libraries.lombok)
}

sourceSets {
    "hoopoeFacade" {}
    "main" {
        compileClasspath += sourceSets["hoopoeFacade"].output
        runtimeClasspath += sourceSets["hoopoeFacade"].output
    }
    "test" {
        compileClasspath += sourceSets["hoopoeFacade"].output
        runtimeClasspath += sourceSets["hoopoeFacade"].output
    }
}

tasks {
    task<Jar>("prepareBootstrapJar") {
        archiveName = "hoopoe-facade.jar"
        from(sourceSets["hoopoeFacade"].output)
        destinationDir = sourceSets["main"].output.resourcesDir
    }

    "jar" {
        dependsOn("prepareBootstrapJar")
    }

    "compileTestJava" {
        dependsOn("prepareBootstrapJar")
    }
}