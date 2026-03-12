package com.ioki.sentry.proguard.gradle.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerAddSentryMetadataToManifestTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerDownloadSentryCliTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerUploadUuidToSentryTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.UUID

private const val SENTRY_CLI_FILE_PATH = "sentry/cli"

class SentryProguardGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val sentryProguardExtension = project.extensions.createSentryProguardExtension()
        val bundledCliVersion = object {}.javaClass.getResource("/SENTRY_CLI_VERSION").readText()
        sentryProguardExtension.cliConfig.version.convention(bundledCliVersion)
        sentryProguardExtension.cliConfig.command.convention(SentryCliConfig.DEFAULT_COMMAND)

        project.replaceSentryProguardUuidInAndroidManifest(extension, sentryProguardExtension)
    }
}

private fun Project.replaceSentryProguardUuidInAndroidManifest(
    extension: AndroidComponentsExtension<*, *, *>,
    sentryProguardExtension: SentryProguardExtension,
) {
    val downloadSentryCliTask = tasks.registerDownloadSentryCliTask(
        cliFilePath = layout.buildDirectory.file(SENTRY_CLI_FILE_PATH),
        cliVersion = sentryProguardExtension.cliConfig.version,
    )

    extension.onVariants { variant ->
        val minifyEnabled = (variant as? ApplicationVariant)?.isMinifyEnabled == true
        if (minifyEnabled) {
            val uuid = UUID.randomUUID().toString()

            val metadataToManifestTask = tasks.registerAddSentryMetadataToManifestTask(
                variantName = variant.name,
                uuid = uuid
            )

            variant.artifacts.use(metadataToManifestTask)
                .wiredWithFiles(
                    { it.mergedManifest },
                    { it.updatedManifest }
                )
                .toTransform(SingleArtifact.MERGED_MANIFEST)

            tasks.registerUploadUuidToSentryTask(
                variantName = variant.name,
                uuid = uuid,
                downloadSentryCliTask = downloadSentryCliTask,
                sentryProguardExtension = sentryProguardExtension,
            )
        }
    }
}