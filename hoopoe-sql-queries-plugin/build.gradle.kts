import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.*
import hoopoe.gradle.plugin.*
import io.spring.gradle.dependencymanagement.dsl.*
import org.springframework.boot.gradle.tasks.*
import org.springframework.boot.gradle.tasks.bundling.BootJar
import hoopoe.gradle.buildscript.*

buildscript {
    repositories {
        mavenCentral()
        //TODO remove when boot 2 is released (apr. Dec 2017)
        maven("https://repo.spring.io/libs-milestone")
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.0.M5")
    }
}

apply {
    plugin("io.spring.dependency-management")
    plugin("hoopoe-assemble-plugin")
}

repositories {
    //TODO remove when boot 2 is released (apr. Dec 2017)
    maven("https://repo.spring.io/libs-milestone")
}

val sourceSets = project.properties["sourceSets"] as SourceSetContainer
val itestAppSourceSet = sourceSets.create("itestApp")
val itestSourceSet = sourceSets.getByName("itest")

configurations {
    "itestAppCompile" {
        exclude(module = "spring-boot-starter-tomcat")
    }
}

configure<DependencyManagementConfigurer> {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.0.0.M5")
    }
}

dependencies {
    "compileOnly"(libraries.slf4j)
    "compileOnly"(project(":hoopoe-api"))
    "compileOnly"(libraries.lombok)

    "itestAppCompile"("org.springframework.boot:spring-boot-starter-jdbc")
    "itestAppCompile"("org.springframework.boot:spring-boot-starter-web")
    "itestAppCompile"("org.springframework.boot:spring-boot-starter-jetty")
    "itestAppCompile"("mysql:mysql-connector-java:5.1.44")
    "itestAppCompile"("org.postgresql:postgresql:42.1.4")

    "itestCompile"(libraries.junit)
    "itestCompile"(libraries.junitDataProvider)
    "itestCompile"(project(":hoopoe-integration-testing"))
    "itestCompile"(libraries.lombok)
    "itestCompile"(libraries.hamcrest)
}

configure<HoopoeAssemblyConfig> {
    pluginClass = "hoopoe.plugins.SqlQueriesPlugin"
}

tasks {
    task<BootJar>("itestAppBootJar") {
        classpath(itestAppSourceSet.runtimeClasspath)
        mainClass = "hoopoe.plugins.sql.app.SqlPluginTestApp"
    }

    task<Copy>("copyItestApp") {
        from(tasks["itestAppBootJar"])
        into(itestSourceSet.output.resourcesDir)
        rename { "itest-app.jar" }
    }

    task<Copy>("copyPlugin") {
       from(tasks["assembleHoopoeZip"])
        into(itestSourceSet.output.resourcesDir)
        rename { "sql-plugin.zip" }
    }

    "itestClasses" {
        dependsOn("copyItestApp", "copyPlugin")
    }

    "integration-tests" {
        onlyIf { System.getenv("ITEST_DB") != null }
    }
}