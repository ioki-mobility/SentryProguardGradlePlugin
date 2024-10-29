
# 🎉 SentryProguardGradlePlugin

[![CI](https://github.com/ioki-mobility/SentryProguardGradlePlugin/actions/workflows/test-plugin.yml/badge.svg)](https://github.com/ioki-mobility/SentryProguardGradlePlugin/actions/workflows/test-plugin.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.ioki.sentry.proguard/sentry-proguard-gradle-plugin?labelColor=%2324292E&color=%233246c8)](https://central.sonatype.com/namespace/com.ioki.sentry.proguard)
[![Snapshot](https://img.shields.io/nexus/s/com.ioki.sentry.proguard/sentry-proguard-gradle-plugin?labelColor=%2324292E&color=%234f78ff&server=https://s01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/ioki/sentry/proguard)
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg?labelColor=%2324292E&color=%23d11064)](https://github.com/ioki-mobility/SentryProguardGradlePlugin/blob/main/LICENSE.md)

🚀 A Gradle plugin that generates `UUIDs`, adds them to your `AndroidManifest.xml`, and uploads the `UUID` along with the generated `mapping` file to Sentry.

## 📚 Table of Contents

- [🔧 Usage](#usage)
  - [🔌 Apply the plugin](#apply-the-plugin)
- [🔍 How it works under the hood](#how-it-works-under-the-hood)
- [🧪 Testing](#testing)
- [🚀 Release](#release)
  - [🌟 Snapshot release](#snapshot-release)
  - [✅ Proper release](#proper-release)

## 🔧 Usage

### 🔌 Apply the plugin

Add the plugin to your **Android application** `build.gradle[.kts]` file and configure it:

```groovy
plugins {
    id "com.ioki.sentry.proguard" version "[CURRENT_VERSION]"
}

sentryProguard {
    organization.set("SENTRY_ORG")
    project.set("SENTRY_PROJECT")
    authToken.set("SENTRY_AUTH_TOKEN")
    noUpload.set(false)
}
```

The `sentryProguard.noUpload` function is useful for development purposes. Normally, you don't want to upload the mapping file to Sentry while creating a minified version on developer machines. Instead, you just want to upload the mapping file on your CI during a "real release." You can implement it like this:

```groovy
def sentryUpload = hasProperty("SENTRY_UPLOAD")
sentryProguard {
    noUpload.set(!sentryUpload)
}
```

By default, if you don’t set the [Gradle property](https://docs.gradle.org/8.0.2/userguide/build_environment.html#sec:gradle_configuration_properties), the plugin won’t upload the mapping files. On your CI, however, you set the property, and therefore the mapping file will be uploaded.

## 🔍 How it works under the hood

When you run "any" task on a [`minifiedEnabled`](https://developer.android.com/reference/tools/gradle-api/8.0/com/android/build/api/variant/CanMinifyCode) [build type](https://developer.android.com/studio/build/build-variants#build-types), the Plugin will:
* Generate a `UUID` 📦
* Place a `<meta-data>` attribute in the `AndroidManifest.xml` (see also this [sentry-android-gradle-plugin code](https://github.com/getsentry/sentry-android-gradle-plugin/blob/fa322a5060fb29073006d4e0d2cb2c2b4eb39aaf/plugin-build/src/main/kotlin/io/sentry/android/gradle/ManifestWriter.kt#L11))
* Create a task to download the Sentry CLI 💻
* Create a task for each build variant that uploads the `UUID` along with the `mapping` file via the [Sentry CLI](https://docs.sentry.io/product/cli/)
* Hook the created tasks into the task graph (adds a `finalizedBy(uploadUuidTask)` to the `minify[BuildVariant]WithR8` task)

## 🧪 Testing

To run the tests, you either have to provide the `ANDROID_HOME` environment variable (pointing to the Android SDK path) or add a `local.properties` file to the `androidTestProject`:

```
// on macOS mostly at: ~/Library/Android/sdk
sdk.dir=[path/to/the/android/sdk]
```

## 🚀 Release

### 🌟 Snapshot release

By default, each merge to the `main` branch will create a new SNAPSHOT release. If you want to use the latest and greatest, use the SNAPSHOT version of the plugin. Please be aware that they might contain bugs or behavior changes.

To use the SNAPSHOT version, include the Sonatype snapshot repository in your `settings.gradle[.kts]`:

```kotlin
pluginManagement {
    repositories {
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
```

### ✅ Proper release

1. Checkout the `main` branch
2. Update the `version` in [`build.gradle.kts`](build.gradle.kts)
3. Update the version for the `consuming of plugin marker publication via mavenLocal works` test
4. Commit 
   * `git commit -m "Prepare next release"`
5. Tag the version with the same version and push it to origin
   * `git tag [VERSION]`
   * `git push origin [VERSION]`
6. Update the version to the "next **minor** version" (including `-SNAPSHOT`)
7. Update the version for the `consuming of plugin marker publication via mavenLocal works` test
8. Commit and push
9. Create a new [GitHub release](https://github.com/ioki-mobility/SentryProguardGradlePlugin/releases/new)
