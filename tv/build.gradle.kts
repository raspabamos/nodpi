plugins {
    alias(libs.plugins.android.application)
}

android {

    namespace = "org.nodpi.hello"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.nodpi.hello"
        minSdk = 21
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"
        multiDexEnabled = true
        signingConfig = signingConfigs.getByName("debug")
        splits {
            abi {
                isEnable = true
                include(
                    "arm64-v8a",
                    "armeabi-v7a",
                    "x86_64",
                    "x86"
                )
                isUniversalApk = true
            }
        }

    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.leanback.preference)
    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.aar")))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.androidx.work.runtime)

}