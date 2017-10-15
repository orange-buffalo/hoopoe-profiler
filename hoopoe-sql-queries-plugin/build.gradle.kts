import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.*
import hoopoe.gradle.plugin.*
import io.spring.gradle.dependencymanagement.dsl.*
import org.springframework.boot.gradle.tasks.*
import org.springframework.boot.gradle.tasks.bundling.BootJar

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
    "compileOnly"(Libraries.slf4j)
    "compileOnly"(project(":hoopoe-api"))
    "compileOnly"(Libraries.lombok)

    "itestAppCompile"("org.springframework.boot:spring-boot-starter-jdbc")
    "itestAppCompile"("org.springframework.boot:spring-boot-starter-web")
    "itestAppCompile"("org.springframework.boot:spring-boot-starter-jetty")
    "itestAppCompile"("mysql:mysql-connector-java:5.1.44")
    "itestAppCompile"("org.postgresql:postgresql:42.1.4")

    "itestCompile"(Libraries.junit)
    "itestCompile"(Libraries.junitDataProvider)
    "itestCompile"(project(":hoopoe-integration-testing"))
    "itestCompile"(Libraries.lombok)
    "itestCompile"(Libraries.hamcrest)
}

configure<HoopoeAssemblyConfig> {
    pluginClass = "hoopoe.plugins.SqlQueriesPlugin"
}

tasks {
    "itestAppBootJar"(BootJar::class) {
        classpath(itestAppSourceSet.runtimeClasspath)
        mainClass = "hoopoe.plugins.sql.app.SqlPluginTestApp"
    }

    "copyItestApp"(Copy::class) {
        from(tasks["itestAppBootJar"])
        into(itestSourceSet.output.resourcesDir)
        rename { "itest-app.jar" }
    }

    "copyPlugin"(Copy::class) {
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