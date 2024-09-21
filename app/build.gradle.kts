import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

val props = Properties().apply {
    load(FileInputStream(rootProject.file("app/keystore.properties")))
}

android {
    namespace = "com.milan.newapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.milan.newapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = props.getProperty("KEY_ALIAS")
            keyPassword = props.getProperty("KEY_PASSWORD")
            storeFile = file(props.getProperty("KEYSTORE_FILE"))
            storePassword = props.getProperty("KEYSTORE_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
        }
        create("release") {
            keyAlias = props.getProperty("KEY_ALIAS")
            keyPassword = props.getProperty("KEY_PASSWORD")
            storeFile = file(props.getProperty("KEYSTORE_FILE"))
            storePassword = props.getProperty("KEYSTORE_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.elvishew:xlog:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-service:2.3.1")
    implementation("com.amap.api:3dmap:6.9.2")
    implementation("com.amap.api:location:4.7.0")
    implementation("com.amap.api:search:6.9.2")
}