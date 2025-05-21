import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.ktlint

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    id("org.jetbrains.dokka") version "2.0.0"
    alias(libs.plugins.google.services)
    id("com.github.ben-manes.versions") version "0.36.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    id("com.google.dagger.hilt.android") version "2.56.2"
    id("kotlin-kapt")
}

ktlint {
    version.set("0.48.2")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    ignoreFailures.set(true)
    disabledRules.set(setOf("no-wildcard-imports", "import-ordering"))
}

android {
    namespace = "com.example.neutralnews_android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.neutralnews_android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resourceConfigurations.addAll(listOf("en", "es"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
                arguments["room.expandProjection"] = "true"
            }
        }
    }
    packagingOptions {
        resources {
            excludes += "META-INF/*.version"
        }
    }
    kotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-P", "plugin:androidx.room.RoomProcessor:roomKspMode=true")
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            buildConfigField("boolean", "EnableAnim", "true")
            buildConfigField("boolean", "IS_TEST_ENVIRONMENT", "false")
        }
        debug {
            buildConfigField("boolean", "EnableAnim", "true")
            buildConfigField("boolean", "IS_TEST_ENVIRONMENT", "true")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx) {
        exclude(group = "com.android.support", module = "support-compat")
    }
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.espresso.core)
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.androidx.browser)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.retrofit)
    implementation(libs.converter.scalars)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit2.kotlin.coroutines.adapter)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    implementation(libs.glide)

    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    implementation(libs.compressor)

    implementation(libs.localization)

    implementation(libs.lottie)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.cookiebar2)

    implementation(libs.viewpagerindicator)
    implementation(libs.circleindicator)

    implementation(libs.glide.transformations)

    implementation(libs.roundedimageview)

    implementation(libs.rangeseekbar) {
        exclude(group = "com.android.support", module = "support-compat")
    }

    implementation(libs.shimmer)

    implementation(libs.filepicker)

    implementation(libs.simpleratingbar)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.ima)

    implementation(libs.unity.ads)

    implementation(libs.core)

    implementation(libs.expandablelayout)

    implementation(libs.billingclient.billing.ktx)
    implementation(libs.integrity)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.transport.api)
    implementation(libs.androidbrowserhelper)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx.v2110)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.blurview)
    implementation(libs.blurry)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v270)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.fragment)
    implementation(libs.kotlinx.metadata.jvm)
    implementation(libs.firebase.firestore.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.play.services.base)
    implementation(libs.play.services.auth.v1802)

    ksp(libs.glide.compiler)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

}