package com.ioki.sentry.proguard.gradle.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.ioki.sentry.proguard.gradle.plugin.tasks.AddSentryMetadataToManifestTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerAddSentryMetadataToManifestTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerDownloadSentryCliTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerUploadUuidToSentryTask
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

private const val SENTRY_CLI_FILE_PATH = "sentry/cli"

class SentryProguardGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        val sentryProguardExtension = project.extensions.createSentryProguardExtension()
        project.replaceSentryProguardUuidInAndroidManifest(extension, sentryProguardExtension)
    }
}

private fun Project.replaceSentryProguardUuidInAndroidManifest(
    extension: AndroidComponentsExtension<*, *, *>,
    sentryProguardExtension: SentryProguardExtension,
) {
    val downloadSentryCliTask = tasks.registerDownloadSentryCliTask(
        objects.fileProperty().fileValue(buildDir.resolve(SENTRY_CLI_FILE_PATH))
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