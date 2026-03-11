package com.ioki.sentry.proguard.gradle.plugin

import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.AUTH_TOKEN
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.CLI_FILE_PATH
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.MAPPING_FILE_PATH
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.ORG
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.PROJECT
import com.ioki.sentry.proguard.gradle.plugin.SentryCliConfig.PlaceHolder.UUID
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

internal fun ExtensionContainer.createSentryProguardExtension(): SentryProguardExtension =
    create("sentryProguard", SentryProguardExtension::class.java)

interface SentryProguardExtension {
    val organization: Property<String>

    val project: Property<String>

    val authToken: Property<String>

    val noUpload: Property<Boolean>

    @get:Nested
    val cliConfig: SentryCliConfig

    fun cliConfig(action: Action<SentryCliConfig>) {
        action.execute(cliConfig)
    }
}

interface SentryCliConfig {
    companion object PlaceHolder {
        const val CLI_FILE_PATH = "{cliFilePath}"
        const val UUID = "{uuid}"
        const val MAPPING_FILE_PATH = "{mappingFilePath}"
        const val ORG = "{org}"
        const val PROJECT = "{project}"
        const val AUTH_TOKEN = "{authToken}"

        internal const val DEFAULT_COMMAND =
            "$CLI_FILE_PATH upload-proguard --uuid $UUID $MAPPING_FILE_PATH --org $ORG --project $PROJECT --auth-token $AUTH_TOKEN"
    }

    val version: Property<String>

    val command: Property<Command>
}

typealias Command = String

internal fun Command.build(
    cliFilePath: String,
    uuid: String,
    mappingFilePath: String,
    org: String,
    project: String,
    authToken: String
): List<String> {
    return this.replace(CLI_FILE_PATH, cliFilePath)
        .replace(UUID, uuid)
        .replace(MAPPING_FILE_PATH, mappingFilePath)
        .replace(ORG, org)
        .replace(PROJECT, project)
        .replace(AUTH_TOKEN, authToken)
        .split("\\s+".toRegex())
}