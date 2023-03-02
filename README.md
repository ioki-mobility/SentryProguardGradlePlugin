# SentryProguardGradlePlugin

A Gradle plugin that generated `UUIDs`, add it to your `AndroidManifest.xml` 
and uploads the `UUID` together with the generated `mapping` file to Sentry.     

## Usage

### Apply the plugin

Add [JitPack](https://jitpack.io/) to the `settings.gradle[.kts]` file:
```groovy
pluginManagement {
    repositories {
        maven { url("https://jitpack.io") }
        resolutionStrategy {
            it.eachPlugin {
                if (requested.id.id == "com.ioki.sentry.proguard") {
                    useModule(
                        "com.github.ioki-mobility.SentryProguardGradlePlugin:${requested.id.id}.gradle.plugin:${requested.version}"
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
}
```
> **Note**: The version could be either a (git) tag (recommended), branch name or hash 

### Add a AndroidManifest placeholder

```xml
<meta-data
    android:name="io.sentry.proguard-uuid"
    android:value="${sentryProguardUuid}"
/>
```

## How it works under the hood

If you run "any" task on the `release` build type, the Plugin will:
* Generate a `UUID`
* Replace the `AndroidManifest` placeholder with it
* Create a task to download the Sentry CLI
* Create a task for each build variant that uploads the `UUID` along with the `mapping` file via the Sentry CL
* Hook those tasks into the task graph (adds a `finalizedBy(uploadUuidTask)` to the `minify[BuildVariant]WithR8` task )

# Testing

To run the tests you either have to provide the `ANDROID_HOME` environment variable (pointing to the Android SDK path) 
or adding a `local.properties` file to the `androidTestProject`:
```
// on macOS mostly at: ~/Library/Android/sdk
sdk.dir=[path/to/the/android/sdk]
```