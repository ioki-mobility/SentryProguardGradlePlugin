import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.androidGradlePlugin)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.strikt)
}

gradlePlugin {
    plugins.register("com.ioki.sentry.proguard") {
        id = "com.ioki.sentry.proguard"
        implementationClass = "com.ioki.sentry.proguard.gradle.plugin.SentryProguardGradlePlugin"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

version = "1.0.0"
group = "com.ioki"
publishing {
    publications {
        register("pluginMaven", MavenPublication::class.java) {
            artifactId = "sentry-proguard"
            pom {
                url.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin/LICENSE.md")
                    }
                }
                organization {
                    name.set("ioki")
                    url.set("https://ioki.com")
                }
                developers {
                    developer {
                        name.set("Stefan 'StefMa' M.")
                        email.set("StefMaDev@outlook.com")
                        url.set("https://StefMa.guru")
                        organization.set("ioki")
                    }
                }
                scm {
                    url.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin")
                    connection.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin.git")
                    developerConnection.set("git@github.com:ioki-mobility/SentryProguardGradlePlugin.git")
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}