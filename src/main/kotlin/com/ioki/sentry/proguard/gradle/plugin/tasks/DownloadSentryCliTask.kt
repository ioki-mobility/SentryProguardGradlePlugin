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
    val sentryCliVersion = object {}.javaClass.getResource("/SENTRY_CLI_VERSION").readText()
    it.downloadUrl.set(findSentryCliDownloadUrl(sentryCliVersion))
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

private fun findSentryCliDownloadUrl(version: String): String {
    val releaseDownloadsUrl = "https://github.com/getsentry/sentry-cli/releases/download/$version"
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    return when {
        osName.contains("mac") ->
            "$releaseDownloadsUrl/sentry-cli-Darwin-universal"

        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") ->
            "$releaseDownloadsUrl/sentry-cli-Linux-x86_64"

        osName.contains("windows") && System.getProperty("os.arch") in listOf("x86", "ia32") ->
            "$releaseDownloadsUrl/sentry-cli-Windows-i686.exe"

        osName.contains("windows") ->
            "$releaseDownloadsUrl/sentry-cli-Windows-x86_64.exe"

        else -> throw GradleException("We do not support $osName")
    }
}
