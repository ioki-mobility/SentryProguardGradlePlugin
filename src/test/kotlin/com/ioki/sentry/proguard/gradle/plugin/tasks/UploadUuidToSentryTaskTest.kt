package com.ioki.sentry.proguard.gradle.plugin.tasks

import com.ioki.sentry.proguard.gradle.plugin.Command
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class UploadUuidToSentryTaskTest {

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
    fun `upload task executes default command with expected arguments`() {
        prepareFakeCli()
        prepareMappingFileFor("aRelease")

        val result = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments("uploadSentryProguardUuidForARelease", "-x", "downloadSentryCli")
            .build()

        expectThat(result.task(":uploadSentryProguardUuidForARelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        expectThat(result.output).contains("upload-proguard --uuid")
        expectThat(result.output).contains("build/outputs/mapping/aRelease/mapping.txt")
        expectThat(result.output).contains("--org sentryOrg --project sentryProject --auth-token sentryAuthToken")
    }

    @Test
    fun `upload task command can be overridden`() {
        prepareFakeCli()
        prepareMappingFileFor("aRelease")
        overrideUploadCommand("${SentryCliConfig.PlaceHolder.CLI_FILE_PATH} override-command --uuid ${SentryCliConfig.PlaceHolder.UUID} ${SentryCliConfig.PlaceHolder.ORG} ${SentryCliConfig.PlaceHolder.AUTH_TOKEN} ${SentryCliConfig.PlaceHolder.PROJECT}")

        val result = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments("uploadSentryProguardUuidForARelease", "-x", "downloadSentryCli")
            .build()

        expectThat(result.task(":uploadSentryProguardUuidForARelease")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        expectThat(result.output).contains("override-command --uuid")
        expectThat(result.output).contains("sentryOrg sentryAuthToken sentryProject")
    }

    private fun prepareFakeCli() {
        val cliPath = testTmpPath.resolve("build/sentry/cli")
        cliPath.parent.createDirectories()
        cliPath.writeText(
            """
            #!/bin/sh
            echo "$@"
            """.trimIndent()
        )
        expectThat(cliPath.toFile().setExecutable(true)).isTrue()
        expectThat(cliPath.exists()).isTrue()
    }

    private fun prepareMappingFileFor(variantName: String) {
        val mappingFilePath = testTmpPath.resolve("build/outputs/mapping/$variantName/mapping.txt")
        mappingFilePath.parent.createDirectories()
        mappingFilePath.writeText("# test mapping")
    }

    private fun overrideUploadCommand(command: String) {
        val buildFile = testTmpPath.resolve("build.gradle.kts")
        val newBuildFile = buildFile.readText().replace(
            oldValue = """organization.set("sentryOrg")""",
            newValue = """organization.set("sentryOrg")
                cliConfig {
                    command.set("$command")
                }
                """.trimIndent()
        )
        buildFile.writeText(newBuildFile)
    }
}