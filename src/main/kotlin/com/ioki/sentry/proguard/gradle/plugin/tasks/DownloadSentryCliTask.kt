package com.ioki.sentry.proguard.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import java.net.URL
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.deleteIfExists

internal fun TaskContainer.registerDownloadSentryCliTask(
    cliFilePath: Provider<RegularFile>,
    cliVersion: Provider<String>
): TaskProvider<DownloadSentryCliTask> = register("downloadSentryCli", DownloadSentryCliTask::class.java) {
    it.cliFilePath.set(cliFilePath)
    it.cliVersion.set(cliVersion)
    it.osName.set(System.getProperty("os.name").lowercase())
}

internal abstract class DownloadSentryCliTask : DefaultTask() {

    @get:Input
    abstract val cliVersion: Property<String>

    @get:Input
    abstract val osName: Property<String>

    @get:OutputFile
    abstract val cliFilePath: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun downloadSentryCli() {
        val cliDownloadUrl = findSentryCliDownloadUrl(cliVersion.get(), osName.get())
        URL(cliDownloadUrl).openStream().use {
            val cliFile = cliFilePath.asFile.get().toPath()
            cliFile.deleteIfExists()
            Files.copy(it, cliFile)
        }
        execOperations.exec {
            it.commandLine("chmod", "u+x", cliFilePath.asFile.get().absolutePath)
        }
    }

    private fun findSentryCliDownloadUrl(version: String, osName: String): String {
        val releaseDownloadsUrl = "https://github.com/getsentry/sentry-cli/releases/download/$version"
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
}
