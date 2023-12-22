package com.ioki.sentry.proguard.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.net.URL
import java.nio.file.Files
import java.util.*
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

internal fun TaskContainer.registerDownloadSentryCliTask(
    cliFilePath: Provider<RegularFile>
): TaskProvider<DownloadSentryCliTask> = register("downloadSentryCli", DownloadSentryCliTask::class.java) {
    it.downloadUrl.set(findSentryCliDownloadUrl())
    it.cliFilePath.set(cliFilePath)
}

internal abstract class DownloadSentryCliTask : DefaultTask() {

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:OutputFile
    abstract val cliFilePath: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun downloadSentryCli() {
        URL(downloadUrl.get()).openStream().use {
            val cliFile = cliFilePath.asFile.get().toPath()
            cliFile.deleteIfExists()
            Files.copy(it, cliFile)
        }
        execOperations.exec {
            it.commandLine("chmod", "u+x", cliFilePath.asFile.get().absolutePath)
        }
    }
}

private fun findSentryCliDownloadUrl(): String {
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)

    return when {
        osName.contains("mac") && System.getProperty("os.arch") == "x86_64" ->
            "https://github.com/getsentry/sentry-cli/releases/download/2.12.0/sentry-cli-Darwin-x86_64"

        osName.contains("mac") ->
            "https://github.com/getsentry/sentry-cli/releases/download/2.12.0/sentry-cli-Darwin-arm64"

        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") ->
            "https://github.com/getsentry/sentry-cli/releases/download/2.12.0/sentry-cli-Linux-x86_64"

        else -> throw GradleException("We do not support $osName")
    }
}
