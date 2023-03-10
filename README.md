# SentryProguardGradlePlugin

A Gradle plugin that generated `UUIDs`, add it to your `AndroidManifest.xml` 
and uploads the `UUID` together with the generated `mapping` file to Sentry.     

## Usage

### Apply the plugin

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

> **Note**: The `[CURRENT_VERSION]` can either be a (git) tag (recommended), branch name, or hash 

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

### Add a AndroidManifest placeholder

```xml
<meta-data
    android:name="io.sentry.proguard-uuid"
    android:value="${sentryProguardUuid}"
/>
```

## How it works under the hood

If you run "any" task on a [`minifiedEnabled`](https://developer.android.com/reference/tools/gradle-api/8.0/com/android/build/api/variant/CanMinifyCode) [build type](https://developer.android.com/studio/build/build-variants#build-types), the Plugin will:
* Generate a `UUID`
* Replace the `AndroidManifest` placeholder with it
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