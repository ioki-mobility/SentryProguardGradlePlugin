package com.ioki.sentry.proguard.gradle.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerDownloadSentryCliTask
import com.ioki.sentry.proguard.gradle.plugin.tasks.registerUploadUuidToSentryTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

private const val BUILD_TYPE_NAME_RELEASE = "release"
private const val BUILD_TYPE_NAME_DEBUG = "debug"

private const val SENTRY_CLI_FILE_PATH = "sentry/cli"

class SentryProguardGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        project.replaceSentryProguardUuidForReleaseBuildType(extension)
        replaceSentryProguardUuidForDebugBuildType(extension)
    }
}

private fun Project.replaceSentryProguardUuidForReleaseBuildType(
    extension: AndroidComponentsExtension<*, *, *>,
) {
    val downloadSentryCliTask = tasks.registerDownloadSentryCliTask(
        objects.fileProperty().fileValue(buildDir.resolve(SENTRY_CLI_FILE_PATH))
    )

    extension.onVariants(extension.selector().withBuildType(BUILD_TYPE_NAME_RELEASE)) { variant ->
        val uuid = UUID.randomUUID().toString()

        variant.manifestPlaceholders.put("sentryProguardUuid", uuid)

        tasks.registerUploadUuidToSentryTask(
            variantName = variant.name,
            uuid = uuid,
            downloadSentryCliTask = downloadSentryCliTask
        )
    }
}

private fun replaceSentryProguardUuidForDebugBuildType(extension: AndroidComponentsExtension<*, *, *>) {
    extension.onVariants(extension.selector().withBuildType(BUILD_TYPE_NAME_DEBUG)) {
        it.manifestPlaceholders.put("sentryProguardUuid", "")
    }
}