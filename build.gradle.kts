plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.services) apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("nl.littlerobots.version-catalog-update") version "0.8.3"
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath(libs.google.services)
        classpath(libs.hilt.android.gradle.plugin)
    }
}