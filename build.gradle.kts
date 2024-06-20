plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    `java-gradle-plugin`
    `maven-publish`
    signing
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
        displayName = "SentryProguardGradlePlugin"
        description =
            "A Gradle plugin that generated UUIDs, add it to your AndroidManifest.xml and uploads the UUID together with the generated mapping file to Sentry."
    }
}

java {
    withSourcesJar()
}

val dokkaJar = tasks.register<Jar>("dokkaJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

version = "2.3.0-SNAPSHOT"
group = "com.ioki.sentry.proguard"
publishing {
    publications {
        register("pluginMaven", MavenPublication::class.java) {
            artifact(dokkaJar)
            artifactId = "sentry-proguard-gradle-plugin"
        }
        withType<MavenPublication>().configureEach {
            pom {
                name.set("SentryProguardGradlePlugin")
                description.set("A Gradle plugin that generated UUIDs, add it to your AndroidManifest.xml and uploads the UUID together with the generated mapping file to Sentry.")
                url.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
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
                        organizationUrl.set("https://ioki.com")
                    }
                }
                scm {
                    url.set("https://github.com/ioki-mobility/SentryProguardGradlePlugin")
                    connection.set("scm:git:git://github.com/ioki-mobility/SentryProguardGradlePlugin.git")
                    developerConnection.set("scm:git:ssh://git@github.com:ioki-mobility/SentryProguardGradlePlugin.git")
                }
            }
        }
    }

    repositories {
        maven("https://maven.pkg.github.com/ioki-mobility/SentryProguardGradlePlugin") {
            name = "GitHubPackages"
            credentials {
                username = project.findProperty("githubPackages.user") as? String
                password = project.findProperty("githubPackages.key") as? String
            }
        }
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            name = "SonatypeSnapshot"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
            name = "SonatypeStaging"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin.jvmToolchain(17)

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    isRequired = hasProperty("GPG_SIGNING_REQUIRED")
    if (isRequired) useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}