package com.ioki.sentry.proguard.gradle.plugin.tasks

import com.ioki.sentry.proguard.gradle.plugin.SentryProguardExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

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

    @get:Inject
    abstract val execOperations: ExecOperations

    private val mappingFilePath: Provider<String> = variantName.map {
        "${project.buildDir}/outputs/mapping/$it/mapping.txt"
    }

    @TaskAction
    fun uploadUuidToSentry() {
        val cliFilePath = cliFilePath.get().asFile.absolutePath
        val command = listOf(
            cliFilePath,
            "upload-proguard",
            "--uuid",
            uuid.get(),
            mappingFilePath.get(),
            "--org",
            sentryOrg.get(),
            "--project",
            sentryProject.get(),
            "--auth-token",
            sentryAuthToken.get()
        )
        logger.log(LogLevel.INFO, "Execute the following command:\n$command")
        execOperations.exec {
            it.commandLine(command)
        }
    }
}