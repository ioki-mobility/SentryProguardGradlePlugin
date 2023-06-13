package com.ioki.sentry.proguard.gradle.plugin.tasks

import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.PrintWriter
import java.io.StringWriter

internal fun TaskContainer.registerAddSentryMetadataToManifestTask(
    variantName: String,
    uuid: String
): TaskProvider<AddSentryMetadataToManifestTask> = register(
    "addSentryProguardMetadataTo${variantName.replaceFirstChar { it.titlecase() }}Manifest",
    AddSentryMetadataToManifestTask::class.java
) {
    it.uuid = uuid
}

internal abstract class AddSentryMetadataToManifestTask : DefaultTask() {

    @get:Input
    lateinit var uuid: String

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun addSentryMetadataToAndroidManifest() {
        val xmlParser = XmlParser()
        val manifestXml = xmlParser.parse(mergedManifest.get().asFile)
        val application = manifestXml.get("application") as NodeList
        val sentryProguardNode = Node(
            null,
            "meta-data",
            mapOf(
                "android:name" to "io.sentry.proguard-uuid",
                "android:value" to uuid
            )
        )
        (application.first() as Node).children().add(sentryProguardNode)
        val stringWriter = StringWriter()
        XmlNodePrinter(PrintWriter(stringWriter)).print(manifestXml)
        updatedManifest.get().asFile.writeText(stringWriter.toString())
    }
}