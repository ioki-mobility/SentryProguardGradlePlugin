package com.ioki.sentry.proguard.gradle.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerDownloadSentryCliTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerUploadUuidToSentryTask
import org.gradle.api.Plugin
import org.gradle.api.Project
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

            variant.manifestPlaceholders.put("sentryProguardUuid", uuid)

            tasks.registerUploadUuidToSentryTask(
                variantName = variant.name,
                uuid = uuid,
                downloadSentryCliTask = downloadSentryCliTask,
                sentryProguardExtension = sentryProguardExtension,
            )
        } else {
            variant.manifestPlaceholders.put("sentryProguardUuid", "")
        }
    }
}