# SentryProguardGradlePlugin

A Gradle plugin that generated `UUIDs`, add it to your `AndroidManifest.xml` 
and uploads the `UUID` together with the generated `mapping` file to Sentry.     

## Usage

### Apply the plugin

```groovy
plugins {
    id "com.ioki.sentry.proguard"
}
```

### Add a AndroidManifest placeholder

```xml
<meta-data
    android:name="io.sentry.proguard-uuid"
    android:value="${sentryProguardUuid}"
/>
```

### Add Gradle properties

```
IOKI_SENTRY_ORG=[SENTRY_ORG]
IOKI_SENTRY_PROJECT=[SENTRY_PROJECT]
IOKI_SENTRY_AUTH_TOKEN=[SENTRY_ORG_AUTH_TOKEN]
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