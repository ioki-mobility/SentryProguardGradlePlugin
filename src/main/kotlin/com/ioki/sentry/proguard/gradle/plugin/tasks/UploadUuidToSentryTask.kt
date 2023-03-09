package com.ioki.sentry.proguard.gradle.plugin.tasks

import com.ioki.sentry.proguard.gradle.plugin.SentryProguardExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

internal fun TaskContainer.registerUploadUuidToSentryTask(
    variantName: String,
    uuid: String,
    downloadSentryCliTask: TaskProvider<DownloadSentryCliTask>,
    sentryProguardExtension: SentryProguardExtension
): TaskProvider<UploadUuidToSentryTask> {
    val uploadUuidTask = register(
        "uploadSentryProguardUuidFor${variantName.replaceFirstChar { it.titlecase() }}",
        UploadUuidToSentryTask::class.java
    ) {
        it.sentryOrg.set(sentryProguardExtension.organization)
        it.sentryProject.set(sentryProguardExtension.project)
        it.sentryAuthToken.set(sentryProguardExtension.authToken)
        it.noUpload.set(sentryProguardExtension.noUpload)
        it.cliFilePath.set(downloadSentryCliTask.flatMap { it.cliFilePath })
        it.uuid = uuid
        it.variantName = variantName
    }

    configureEach { task ->
        if (task.name == "minify${variantName.replaceFirstChar { it.titlecase() }}WithR8") {
            task.finalizedBy(uploadUuidTask)
        }
    }

    return uploadUuidTask
}

internal abstract class UploadUuidToSentryTask : DefaultTask() {

    @Input
    lateinit var variantName: String

    @Input
    lateinit var uuid: String

    @get:Input
    abstract val sentryOrg: Property<String>

    @get:Input
    abstract val sentryProject: Property<String>

    @get:Input
    abstract val sentryAuthToken: Property<String>

    @get:Optional
    @get:Input
    abstract val noUpload: Property<Boolean>

    @InputFile
    val cliFilePath: RegularFileProperty = project.objects.fileProperty()

    private val COMMAND = "%s upload-proguard --uuid %s %s --org %s --project %s --auth-token %s"

    @TaskAction
    fun uploadUuidToSentry() {
        val cliFilePath = cliFilePath.get().asFile
        val uuid = uuid
        val mappingFilePath = "${project.buildDir}/outputs/mapping/${variantName}/mapping.txt"
        val command = COMMAND.format(
            cliFilePath,
            uuid,
            mappingFilePath,
            sentryOrg.get(),
            sentryProject.get(),
            sentryAuthToken.get()
        )
        val noUpload = noUpload.getOrElse(false)
        if (noUpload) {
            logger.log(LogLevel.INFO, "No upload cause sentryProguard.noUpload were set.")
        } else {
            logger.log(LogLevel.INFO, "Execute the following command:\n$command")
            val process = Runtime.getRuntime().exec(command)
            val stdIn = process.inputStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
            val stdErr = process.errorStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
            if (stdErr.isNotBlank()) throw GradleException("$stdIn \n $stdErr")
            else println(stdIn)
        }
    }
}