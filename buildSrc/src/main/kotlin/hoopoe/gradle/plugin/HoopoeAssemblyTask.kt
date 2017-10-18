package hoopoe.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.*
import java.io.File
import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class HoopoeAssemblyTask : DefaultTask() {

    @get:OutputFile
    var outputArchive: Callable<File> = Callable { File(temporaryDir, archiveName.call()) }

    @get:Input
    @Optional
    var pluginClassName: Callable<String?> = Callable { null }

    @get:Input
    @Optional
    var extensionClassName: Callable<String?> = Callable { null }

    @get:Input
    var archiveName: Callable<String> = Callable { "${project.name}.zip" }

    @get:InputFiles
    var classesDirs: Callable<Iterable<File>> = Callable {
        HoopoeAssemblyTask.resolveClassesAndResourcesBySourceSet(project, "main")
    }

    @get:InputFiles
    var libFiles: Callable<Iterable<File>> = Callable {
        HoopoeAssemblyTask.resolveLibsByConfiguration(project, "runtime")
    }

    init {
    }

    @Suppress("unused")
    @TaskAction
    fun buildArchive() {
        val manifestFile = createManifest()

        val archiveFile = outputArchive.call()
        logger.debug("starting creation of $archiveFile")

        ZipOutputStream(archiveFile.outputStream().buffered()).use { archiveStream ->

            for (classesDir in classesDirs.call()) {
                for (classFile in classesDir.walk().asSequence()) {
                    val relativePath = classFile.relativeTo(classesDir)
                    writeFileToArchive(classFile, "classes/$relativePath", archiveStream)
                }
            }

            manifestFile?.let {
                writeFileToArchive(it, "META-INF/hoopoe.properties", archiveStream)
            }

            for (libFile in libFiles.call()) {
                writeFileToArchive(libFile, "lib/${libFile.name}", archiveStream)
            }
        }
        logger.debug("archive has been created")
    }

    private fun writeFileToArchive(file: File, archivePath: String, archiveStream: ZipOutputStream) {
        if (!file.isDirectory) {
            val archiveEntry = ZipEntry(archivePath)
            archiveStream.putNextEntry(archiveEntry)
            file.inputStream().buffered().use {
                it.copyTo(archiveStream)
            }
            archiveStream.closeEntry()
        }
    }

    private fun createManifest(): File? {
        val plugin = pluginClassName.call()
        val extension = extensionClassName.call()
        if (plugin != null || extension != null) {
            val manifestFile = File(temporaryDir, "hoopoe.properties")

            manifestFile.parentFile.mkdirs()

            plugin?.let {
                manifestFile.writeText("plugin.className=$it")

                logger.debug("created manifest entry for plugin $it")
            }

            extension?.let {
                manifestFile.writeText("extension.className=$it")

                logger.debug("created manifest entry for extension $it")
            }

            return manifestFile
        }
        return null
    }

    companion object CommonResolvers {

        /**
         * Returns iterable over libraries defined in dependencies of specified configuration
         */
        fun resolveLibsByConfiguration(project: Project, configurationName: String): Iterable<File> {
            return project.configurations.getByName(configurationName)
                    .resolvedConfiguration.resolvedArtifacts
                    .map(ResolvedArtifact::getFile)
        }

        /**
         * Returns iterable over directories where compiled classes and resources are stored
         */
        fun resolveClassesAndResourcesBySourceSet(project: Project, sourceSetName: String): Iterable<File> {
            val sourceSets = project.properties["sourceSets"] as SourceSetContainer
            val sourceSet = sourceSets.asMap[sourceSetName]
                    ?: throw IllegalArgumentException("Cannot find source set $sourceSetName")

            val dirs = arrayListOf<File>()
            dirs.addAll(sourceSet.output.classesDirs.files)
            dirs.add(sourceSet.output.resourcesDir)

            return dirs
        }
    }
}