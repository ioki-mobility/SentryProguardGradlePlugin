package com.ioki.sentry.proguard.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively

class SentryProguardGradlePluginTest {

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
    fun `running assembleRelease should run tasks in correct sequence`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleRelease",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        val downloadCliTask = result.task(":downloadSentryCli")
        val uploadForAReleaseTask = result.task(":uploadSentryProguardUuidForARelease")
        val uploadForBReleaseTask = result.task(":uploadSentryProguardUuidForBRelease")
        val uploadForCReleaseTask = result.task(":uploadSentryProguardUuidForCRelease")
        val filteredTasks = result.tasks.filter {
            it == downloadCliTask
                    || it == uploadForAReleaseTask
                    || it == uploadForBReleaseTask
                    || it == uploadForCReleaseTask
        }
        expectThat(filteredTasks).containsSequence(
            downloadCliTask,
            uploadForAReleaseTask,
            uploadForBReleaseTask,
            uploadForCReleaseTask
        )
    }

    @Test
    fun `running assembleProduction should run tasks in correct sequence`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleProduction",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        val downloadCliTask = result.task(":downloadSentryCli")
        val uploadForAProductionTask = result.task(":uploadSentryProguardUuidForAProduction")
        val uploadForBProductionTask = result.task(":uploadSentryProguardUuidForBProduction")
        val uploadForCProductionTask = result.task(":uploadSentryProguardUuidForCProduction")
        val filteredTasks = result.tasks.filter {
            it == downloadCliTask
                    || it == uploadForAProductionTask
                    || it == uploadForBProductionTask
                    || it == uploadForCProductionTask
        }
        expectThat(filteredTasks).containsSequence(
            downloadCliTask,
            uploadForAProductionTask,
            uploadForBProductionTask,
            uploadForCProductionTask
        )
    }

    @Test
    fun `running assembleADebug should not run any sentry related task`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(listOf("assembleADebug"))
            .build()

        val downloadCliTask = result.task(":downloadSentryCli")
        val filteredTasks = result.tasks.filter {
            it == downloadCliTask || it.path.contains("uploadSentryProguardUuidFor")
        }
        expectThat(filteredTasks).isEmpty()
    }

    @Test
    fun `running assembleBStaging should not run any sentry related task`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(listOf("assembleBStaging"))
            .build()

        val downloadCliTask = result.task(":downloadSentryCli")
        val filteredTasks = result.tasks.filter {
            it == downloadCliTask || it.path.contains("uploadSentryProguardUuidFor")
        }
        expectThat(filteredTasks).isEmpty()
    }

    @Test
    fun `running assembleRelease should fail with expected upload`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(listOf("assembleRelease"))
            .buildAndFail()

        expectThat(result.task(":downloadSentryCli")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        expectThat(result.task(":uploadSentryProguardUuidForARelease")!!.outcome)
            .isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `downloadSentryCli task should be up-to-date when run multiple times`() {
        val firstTime: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(listOf("downloadSentryCli"))
            .build()
        expectThat(firstTime.task(":downloadSentryCli")).isNotNull()
            .get { outcome }
            .isEqualTo(TaskOutcome.SUCCESS)

        val secondTime: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(listOf("downloadSentryCli"))
            .build()

        expectThat(secondTime.task(":downloadSentryCli")).isNotNull()
            .get { outcome }
            .isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `running assembleARelease with noUpload should skip upload task`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleARelease",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        val uploadForAReleaseTask = result.task(":uploadSentryProguardUuidForARelease")
        expectThat(uploadForAReleaseTask!!.outcome).isEqualTo(TaskOutcome.SKIPPED)
    }

    @Test
    fun `running assembleARelease with info should log executed command`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleARelease",
                    "--info"
                )
            )
            .buildAndFail()

        val cliPath = testTmpPath.toAbsolutePath().resolve("build/sentry/cli")
        val mappingFilePath = testTmpPath.toAbsolutePath().resolve("build/outputs/mapping/aRelease/mapping.txt")
        val uuidRegexPattern = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"
        expectThat(result.output) {
            contains("Execute the following command:")
            contains("$cliPath upload-proguard --uuid $uuidRegexPattern".toRegex())
            contains("$mappingFilePath --org sentryOrg --project sentryProject --auth-token sentryAuthToken")
        }
    }

}