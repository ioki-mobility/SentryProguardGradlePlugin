package com.ioki.sentry.proguard.gradle.plugin.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DownloadSentryCliTaskTest {

    @TempDir
    lateinit var testTmpPath: Path

    @BeforeEach
    @OptIn(ExperimentalPathApi::class)
    fun moveTestProjectToTestTmpDir() {
        val testProjectPath = Paths.get(System.getProperty("user.dir"), "androidTestProject")
        testProjectPath.copyToRecursively(
            testTmpPath,
            overwrite = true,
            followLinks = false
        )
    }

    @Test
    fun `downloadSentryCli task downloads sentry cli binary`() {
        val result = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments("downloadSentryCli")
            .build()

        expectThat(result.task(":downloadSentryCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val cliPath = testTmpPath.resolve("build/sentry/cli")
        expectThat(cliPath.exists()).isTrue()
        expectThat(cliPath.fileSize()).isGreaterThan(0)
        expectThat(cliPath.toFile().canExecute()).isTrue()
    }

    @Test
    fun `downloaded sentry cli is executable and returns version`() {
        GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments("downloadSentryCli")
            .build()

        val cliPath = testTmpPath.resolve("build/sentry/cli")
        val process = ProcessBuilder(cliPath.toAbsolutePath().toString(), "--version")
            .directory(testTmpPath.toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        expectThat(exitCode).isEqualTo(0)
        val bundledVersion = object {}.javaClass.getResource("/SENTRY_CLI_VERSION").readText()
        expectThat(output.lowercase()).contains("sentry-cli $bundledVersion")
    }

    @Test
    fun `custom sentry cli version is downloaded executed and returns version`() {
        val buildFile = testTmpPath.resolve("build.gradle.kts")
        val newBuildFile = buildFile.readText().replace(
            oldValue = """organization.set("sentryOrg")""",
            newValue = """organization.set("sentryOrg")
                cliConfig {
                    version.set("2.0.0")
                }
                """.trimIndent()
        )
        buildFile.writeText(newBuildFile)
        GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments("downloadSentryCli")
            .build()

        val cliPath = testTmpPath.resolve("build/sentry/cli")
        val process = ProcessBuilder(cliPath.toAbsolutePath().toString(), "--version")
            .directory(testTmpPath.toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        expectThat(exitCode).isEqualTo(0)
        expectThat(output.lowercase()).contains("sentry-cli 2.0.0")
    }
}