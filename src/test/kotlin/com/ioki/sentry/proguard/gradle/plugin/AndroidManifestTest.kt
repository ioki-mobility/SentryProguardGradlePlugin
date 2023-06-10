package com.ioki.sentry.proguard.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText

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
        val androidManifest = testTmpPath
            .resolve("build/intermediates/packaged_manifests/aDebug/AndroidManifest.xml")

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

        expectThat(androidManifest.exists()).isTrue()
        expectThat(androidManifest.readText()).not().contains("android:name=\"io.sentry.proguard-uuid\"")
        expectThat(androidManifest.readText()).not().contains("android:value=\"\"")
    }

    @Test
    fun `on release build android manifest exist and does contain sentry metadata with uuid value`() {
        val androidManifest = testTmpPath
            .resolve("build/intermediates/packaged_manifests/aRelease/AndroidManifest.xml")

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

        expectThat(androidManifest.exists()).isTrue()
        expectThat(androidManifest.readText()).contains("android:name=\"io.sentry.proguard-uuid\"")
        val uuidRegexPattern = "[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}"
        expectThat(androidManifest.readText()).contains("android:value=\"$uuidRegexPattern\"".toRegex())
    }
}