plugins {
    id("com.android.application") version "8.0.2"
    id("com.ioki.sentry.proguard")
}

android {
    compileSdk = 33
    namespace = "com.ioki.sentry.proguard.plugin.test.project"

    defaultConfig {
        applicationId = "com.ioki.sentry.proguard.plugin.test.project"
        minSdk = 29
        targetSdk = 33
        versionName = "0000-00-00.0"
        versionCode = 1
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("production") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("staging") {
            isMinifyEnabled = false
        }
    }

    flavorDimensions += "partner"
    val partners = listOf("a", "b", "c")
    partners.forEach { partnerName ->
        productFlavors.register(partnerName) {
            dimension = "partner"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sentryProguard {
    organization.set("sentryOrg")
    project.set("sentryProject")
    authToken.set("sentryAuthToken")
    noUpload.set(
        providers.gradleProperty("IOKI_SENTRY_NO_UPLOAD")
            .map { it.toBoolean() }
            .orElse(false)
    )
}
