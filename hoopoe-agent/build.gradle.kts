import org.gradle.api.tasks.bundling.Jar
import com.jfrog.bintray.gradle.*

apply {
    plugin("hoopoe-dependency-plugin")
    plugin("com.jfrog.bintray")  //todo verify if this is a proper place to use this plugin
}

dependencies {
    "compile"(project(":hoopoe-classloader"))
    "compile"(project(path = ":hoopoe-core", configuration = "hoopoe"))
    "compile"(project(path = ":hoopoe-sql-queries-plugin", configuration = "hoopoe"))
    "compile"(project(path = ":hoopoe-web-view-extension", configuration = "hoopoe"))
}

val jar: Jar by tasks
jar.apply {
    manifest {
        attributes(mapOf("Premain-Class" to "hoopoe.core.HoopoeAgent"))
    }
    archiveName = "$baseName.$extension"
}

configure<BintrayExtension> {
    pkg(closureOf<BintrayExtension.PackageConfig> {
        name = "hoopoe-profiler"
    })
    filesSpec(closureOf<RecordingCopyTask> {
        from(tasks.findByName("jar"))
        into("${project.version}")
    })
}
