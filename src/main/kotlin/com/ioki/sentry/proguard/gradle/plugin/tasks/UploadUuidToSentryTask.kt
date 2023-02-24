package com.ioki.sentry.proguard.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*

internal fun TaskContainer.registerUploadUuidToSentryTask(
    variantName: String,
    uuid: String,
    downloadSentryCliTask: TaskProvider<DownloadSentryCliTask>
): TaskProvider<UploadUuidToSentryTask> {
    val uploadUuidTask = register(
        "uploadSentryProguardUuidFor${variantName.replaceFirstChar { it.titlecase() }}",
        UploadUuidToSentryTask::class.java
    ) {
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

    @Input
    val sentryOrg = project.property("IOKI_SENTRY_ORG") as String

    @Input
    val sentryProject = project.property("IOKI_SENTRY_PROJECT") as String

    @Input
    val sentryAuthToken = project.property("IOKI_SENTRY_AUTH_TOKEN") as String

    @Input
    val noUpload = (project.findProperty("IOKI_SENTRY_NO_UPLOAD") as? String).toBoolean()

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
            sentryOrg,
            sentryProject,
            sentryAuthToken
        )
        if (!noUpload) {
            val process = Runtime.getRuntime().exec(command)
            val stdIn = process.inputStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
            val stdErr = process.errorStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
            if (stdErr.isNotBlank()) throw GradleException("$stdIn \n $stdErr")
            else println(stdIn)
        } else {
            logger.log(LogLevel.INFO, "No upload cause property `IOKI_SENTRY_NO_UPLOAD` were set.")
        }
    }
}