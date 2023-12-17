# SentryProguardGradlePlugin

[![CI](https://github.com/ioki-mobility/SentryProguardGradlePlugin/actions/workflows/test-plugin.yml/badge.svg)](https://github.com/ioki-mobility/SentryProguardGradlePlugin/actions/workflows/test-plugin.yml)
[![Jitpack](https://jitpack.io/v/ioki-mobility/SentryProguardGradlePlugin.svg)](https://jitpack.io/#ioki-mobility/SentryProguardGradlePlugin)
[![MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ioki-mobility/SentryProguardGradlePlugin/blob/master/LICENSE.md)

A Gradle plugin that generated `UUIDs`, adds it to your `AndroidManifest.xml` 
and uploads the `UUID` together with the generated `mapping` file to Sentry.     

## Usage

### Apply the plugin

Add the plugin to the **Android application** `build.gradle[.kts]` file and configure it:

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

The `sentryProguard.noUpload` function is useful for development purposes.
Normally, you don't want to upload the mapping file to Sentry while creating a minified version on developer machines.
Instead, you just want to upload the mapping file on your CI. In case you do a "real release".
So something like the following can be implemented:

```groovy
def sentryUpload = hasProperty("SENTRY_UPLOAD")
sentryProguard {
    noUpload.set(!sentryUpload)
}
```

By default, you don't set the [Gradle property](https://docs.gradle.org/8.0.2/userguide/build_environment.html#sec:gradle_configuration_properties).
In this case the plugin won't upload the mapping files.
On your CI, however, you set the property and therefore the mapping file will be uploaded.

## How it works under the hood

If you run "any" task on a [`minifiedEnabled`](https://developer.android.com/reference/tools/gradle-api/8.0/com/android/build/api/variant/CanMinifyCode) [build type](https://developer.android.com/studio/build/build-variants#build-types), the Plugin will:
* Generate a `UUID`
* Place a `<meta-data>` attribute to the `AndroidManifest.xml` (see also this [sentry-android-gradle-plugin code](https://github.com/getsentry/sentry-android-gradle-plugin/blob/fa322a5060fb29073006d4e0d2cb2c2b4eb39aaf/plugin-build/src/main/kotlin/io/sentry/android/gradle/ManifestWriter.kt#L11))
* Create a task to download the Sentry CLI
* Create a task for each build variant that uploads the `UUID` along with the `mapping` file via the [Sentry CLI](https://docs.sentry.io/product/cli/)
* Hook the created tasks into the task graph (adds a `finalizedBy(uploadUuidTask)` to the `minify[BuildVariant]WithR8` task)

# Testing

To run the tests you either have to provide the `ANDROID_HOME` environment variable (pointing to the Android SDK path) 
or adding a `local.properties` file to the `androidTestProject`:
```
// on macOS mostly at: ~/Library/Android/sdk
sdk.dir=[path/to/the/android/sdk]
```

# Release

## Snapshot release

By default, each merge to the `main` branch will create a new SNAPSHOT release.
If you want to use the latest and greatest use the SNAPSHOT version of the plugin.
But please be aware that they might contain bugs or behaviour changes.

To use the SNAPSHOT version you have to include the sonatype snapshot repository to your `settings.gradle[.kts]`
```kotlin
pluginManagement {
    repositories {
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}
```

## Proper release

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Update the version for the `consuming of plugin marker publication via mavenLocal works` test
* Commit 
  * `git commit -m "Prepare next relaese"`
* Tag the version with the same version and push it to origin
  * `git tag [VERSION]`
  * `git push origin [VERSION]`
* Update the version to the "next **minor** version" (including `-SNAPSHOT`)
* Update the version for the `consuming of plugin marker publication via mavenLocal works` test
* Commit and push
* Create a new [GitHub release](https://github.com/ioki-mobility/SentryProguardGradlePlugin/releases/new)
