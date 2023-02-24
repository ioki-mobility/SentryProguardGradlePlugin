package com.ioki.sentry.proguard.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsSequence
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively

@OptIn(ExperimentalPathApi::class)
class SentryProguardGradlePluginTest {

    private var testTmpPath: Path = Files.createTempDirectory("ioki-sentry-test-tmp-dir")

    @BeforeEach
    @OptIn(ExperimentalPathApi::class)
    fun moveTestProjectToTestTmpDir() {
        val testProjectPath = Paths.get(System.getProperty("user.dir"), "androidTestProject")
        println(testProjectPath.absolutePathString())
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
                    "-PIOKI_SENTRY_ORG=a",
                    "-PIOKI_SENTRY_PROJECT=b",
                    "-PIOKI_SENTRY_AUTH_TOKEN=c",
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
    fun `running assembleRelease should fail with expected upload`() {
        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleRelease",
                    "-PIOKI_SENTRY_ORG=a",
                    "-PIOKI_SENTRY_PROJECT=b",
                    "-PIOKI_SENTRY_AUTH_TOKEN=c",
                )
            )
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

}