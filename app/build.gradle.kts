plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.letsdoit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.letsdoit"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation ("androidx.media:media:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation ("com.google.code.gson:gson:2.11.0")
    implementation ("org.apache.poi:poi:5.2.3")
    implementation ("org.apache.poi:poi-ooxml:5.2.3")
    implementation ("org.apache.poi:poi-scratchpad:5.2.3")
    
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation ("com.aspose:aspose-words:24.3:android.via.java")
    implementation ("com.aspose:aspose-pdf:24.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")



}