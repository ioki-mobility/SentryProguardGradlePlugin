package com.ioki.sentry.proguard.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsSequence
import strikt.assertions.isEqualTo
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class SentryProguardGradlePluginPublishingTest {

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
    fun `consuming of plugin marker publication via mavenLocal works`() {
        val buildFile = testTmpPath.resolve("build.gradle")
        val newBuildFile = buildFile.readText().replace(
            oldValue = """id "com.ioki.sentry.proguard"""",
            newValue = """id "com.ioki.sentry.proguard" version "2.2.0-SNAPSHOT""""
        )
        buildFile.writeText(newBuildFile)
        val settingsFile = testTmpPath.resolve("settings.gradle")
        val newSettingsFile = settingsFile.readText().replace(
            oldValue = "mavenCentral()",
            newValue = "mavenCentral() \n mavenLocal()"
        )
        settingsFile.writeText(newSettingsFile)

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
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

}
