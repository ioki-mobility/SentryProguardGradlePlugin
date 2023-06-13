package com.ioki.sentry.proguard.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

class AndroidManifestTest {

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
    fun `on debug build android manifest exist and does contain sentry metadata without value`() {
        GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleADebug",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        val packedAndroidManifest = testTmpPath
            .resolve("build/intermediates/packaged_manifests/aDebug/AndroidManifest.xml")
        expectThat(packedAndroidManifest.exists()).isTrue()
        expectThat(packedAndroidManifest.readText()).not().contains("android:name=\"io.sentry.proguard-uuid\"")
        expectThat(packedAndroidManifest.readText()).not().contains("android:value=\"\"")
    }

    @Test
    fun `on release build android manifest exist and does contain sentry metadata with uuid value`() {
        GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleARelease",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        val packedAndroidManifest = testTmpPath
            .resolve("build/intermediates/packaged_manifests/aRelease/AndroidManifest.xml")
        expectThat(packedAndroidManifest.exists()).isTrue()
        expectThat(packedAndroidManifest.readText()).contains("android:name=\"io.sentry.proguard-uuid\"")
        val uuidRegexPattern = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"
        expectThat(packedAndroidManifest.readText()).contains("android:value=\"$uuidRegexPattern\"".toRegex())
    }

    @Test
    fun `on release build in an library without default android manifest should set to sentry metadata with uuid value to it`() {
        val sourceAndroidManifest = testTmpPath.resolve("src/main/AndroidManifest.xml")
        sourceAndroidManifest.deleteExisting()
        testTmpPath.resolve("build.gradle").apply {
            var newBuildGradle = readText()
            newBuildGradle = newBuildGradle.replace(
                oldValue = "id \"com.android.application\"",
                newValue = "id \"com.android.library\""
            )
            newBuildGradle = newBuildGradle.replace(
                oldValue = "applicationId",
                newValue = "// applicationId"
            )
            writeText(newBuildGradle)
        }

        GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "assembleARelease",
                    "-PIOKI_SENTRY_NO_UPLOAD=true"
                )
            )
            .build()

        expectThat(sourceAndroidManifest.exists()).isFalse()
        val packedAndroidManifest = testTmpPath
            .resolve("build/intermediates/packaged_manifests/aRelease/AndroidManifest.xml")
        expectThat(packedAndroidManifest.exists()).isTrue()
        expectThat(packedAndroidManifest.readText()).contains("android:name=\"io.sentry.proguard-uuid\"")
        val uuidRegexPattern = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"
        expectThat(packedAndroidManifest.readText()).contains("android:value=\"$uuidRegexPattern\"".toRegex())
    }
}