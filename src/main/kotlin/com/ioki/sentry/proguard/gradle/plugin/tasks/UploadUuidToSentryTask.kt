package com.ioki.sentry.proguard.gradle.plugin.tasks

import com.ioki.sentry.proguard.gradle.plugin.SentryProguardExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
        it.onlyIf("sentryProguard.noUpload is set to false") {
            val noUpload = sentryProguardExtension.noUpload.getOrElse(false)
            !noUpload
        }
        it.sentryOrg.set(sentryProguardExtension.organization)
        it.sentryProject.set(sentryProguardExtension.project)
        it.sentryAuthToken.set(sentryProguardExtension.authToken)
        it.cliFilePath.set(downloadSentryCliTask.flatMap { it.cliFilePath })
        it.uuid.set(uuid)
        it.variantName.set(variantName)
    }

    configureEach { task ->
        if (task.name == "minify${variantName.replaceFirstChar { it.titlecase() }}WithR8") {
            task.finalizedBy(uploadUuidTask)
        }
    }

    return uploadUuidTask
}

internal abstract class UploadUuidToSentryTask : DefaultTask() {

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val uuid: Property<String>

    @get:Input
    abstract val sentryOrg: Property<String>

    @get:Input
    abstract val sentryProject: Property<String>

    @get:Input
    abstract val sentryAuthToken: Property<String>

    @get:InputFile
    abstract val cliFilePath: RegularFileProperty

    private val mappingFilePath: Provider<String> = variantName.map {
        "${project.buildDir}/outputs/mapping/$it/mapping.txt"
    }

    private val COMMAND = "%s upload-proguard --uuid %s %s --org %s --project %s --auth-token %s"

    @TaskAction
    fun uploadUuidToSentry() {
        val cliFilePath = cliFilePath.get().asFile
        val command = COMMAND.format(
            cliFilePath,
            uuid.get(),
            mappingFilePath.get(),
            sentryOrg.get(),
            sentryProject.get(),
            sentryAuthToken.get()
        )
        logger.log(LogLevel.INFO, "Execute the following command:\n$command")
        val process = Runtime.getRuntime().exec(command)
        val stdIn = process.inputStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
        val stdErr = process.errorStream.bufferedReader().useLines { it.toList() }.joinToString(separator = "\n")
        if (stdErr.isNotBlank()) throw GradleException("$stdIn \n $stdErr")
        else println(stdIn)
    }
}