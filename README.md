# SentryProguardGradlePlugin

A Gradle plugin that generated `UUIDs`, adds it to your `AndroidManifest.xml` 
and uploads the `UUID` together with the generated `mapping` file to Sentry.     

## Usage

### Apply the plugin

<details open>
<summary>Via GitHub Packages</summary>

Add [GitHub Packages](https://github.com/ioki-mobility/SentryProguardGradlePlugin/packages/) to the `settings.gradle[.kts]` file:

```groovy
pluginManagement {
    repositories {
        maven {
          url("https://maven.pkg.github.com/ioki-mobility/SentryProguardGradlePlugin")
          credentials {
            username = "[GitHub_Username]"
            password = "[GitHub_Secret]"
          }
        }
    }
}
```

</details>

<details>
<summary>Via JitPack</summary>

Add [JitPack](https://jitpack.io/) to the `settings.gradle[.kts]` file:

```groovy
pluginManagement {
    repositories {
        maven { 
            url("https://jitpack.io")
            content {
                includeGroup("com.github.ioki-mobility.SentryProguardGradlePlugin")
            }
        }
        resolutionStrategy {
            it.eachPlugin {
                if (requested.id.id == "com.ioki.sentry.proguard") {
                    useModule(
                        "com.github.ioki-mobility.SentryProguardGradlePlugin:sentry-proguard:${requested.version}"
                    )
                }
            }
        }
    }
}
```

</details>

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

> **Note**: If you use JitPack, the `[CURRENT_VERSION]` can either be a (git) tag (recommended), branch name, or hash 

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

* Checkout `main` branch
* Update the `version` in [`build.gradle.kts`](build.gradle.kts)
* Commit with message `Next version`
* Tag the version with the same version and push it to origin
  * This triggers the `publishToGithubPackages` GitHub Actions 
* (Wait until its published)
* Update the version to the "next **minor** version" (including `-SNAPSHOT`)
* Push to origin
* Create a new [release](https://github.com/ioki-mobility/SentryProguardGradlePlugin/releases/new)