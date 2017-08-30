import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.tasks.*
import java.io.File
import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class HoopoeAssemblyTask : DefaultTask() {

    @OutputFile
    @Optional
    var outputArchive: File? = null

    @Input
    @Optional
    var pluginClassName: String? = null

    @Input
    @Optional
    var extensionClassName: String? = null

    @Input
    @Optional
    var archiveName: String? = null

    @Input
    @Optional
    var archiveClassifier: String? = null

    @Input
    @Optional
    var attachToArtifacts: Boolean? = null

    @Input
    @Optional
    var classesSourceSetName: String? = null

    @Input
    var libFiles: Callable<Collection<File>> = Callable {
        HoopoeAssemblyTask.resolveLibsByConfiguration(project, "runtime")
                .minus(HoopoeAssemblyTask.resolveLibsByConfiguration(project, "provided"))
    }

    init {
    }

    @TaskAction
    fun buildArchive() {
        val manifestFile = createManifest()

        val sourceSet = getSourceSet(classesSourceSetName ?: "main")

        val archiveFile = File(temporaryDir, getEffectiveArchiveName())
        logger.lifecycle("starting creation of $archiveFile")

        ZipOutputStream(archiveFile.outputStream().buffered()).use { archiveStream ->

            for (classesDir in sourceSet.output.classesDirs) {
                for (classFile in classesDir.walk().asSequence()) {
                    val relativePath = classFile.relativeTo(classesDir)
                    writeFileToArchive(classFile, "classes/$relativePath", archiveStream)
                }
            }

            val resourcesDir = sourceSet.output.resourcesDir
            for (resourceFile in resourcesDir.walk().asSequence()) {
                val relativePath = resourceFile.relativeTo(resourcesDir)
                writeFileToArchive(resourceFile, "classes/$relativePath", archiveStream)
            }

            manifestFile?.let {
                writeFileToArchive(it, "META-INF/hoopoe.properties", archiveStream)
            }

            for (libFile in libFiles.call()) {
                writeFileToArchive(libFile, "lib/${libFile.name}", archiveStream)
            }
        }
        logger.lifecycle("archive has been created")

        outputArchive = archiveFile

        if (attachToArtifacts ?: false) {
            val classifier = archiveClassifier ?: "hoopoe"
            project.artifacts.add(classifier, outputArchive)

            logger.lifecycle("attached to artifacts with classifier $classifier")
        }
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

    private fun getEffectiveArchiveName() = archiveName ?: "${project.name}.zip"

    private fun createManifest(): File? {
        if (pluginClassName != null || extensionClassName != null) {
            val manifestFile = File(temporaryDir, "hoopoe.properties")

            manifestFile.parentFile.mkdirs()

            pluginClassName?.let {
                manifestFile.writeText("plugin.className=$it")

                logger.lifecycle("created manifest entry for plugin $it")
            }

            extensionClassName?.let {
                manifestFile.writeText("extension.className=$it")

                logger.lifecycle("created manifest entry for extension $it")
            }

            return manifestFile
        }
        return null
    }

    private fun getSourceSet(name: String): SourceSet {
        val sourceSets = project.properties["sourceSets"] as SourceSetContainer
        val sourceSet = sourceSets.asMap[name]
        return sourceSet ?: throw IllegalArgumentException("Cannot find source set $name")
    }

    companion object CommonResolvers {
        fun resolveLibsByConfiguration(project: Project, configurationName: String): Collection<File> {
            return project.configurations.getByName(configurationName)
                    .resolvedConfiguration.resolvedArtifacts
                    .map(ResolvedArtifact::getFile)
        }
    }
}