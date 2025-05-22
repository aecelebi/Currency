plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.currency"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.currency"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // veya daha yenisi
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // veya daha yenisi
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3") // veya daha yenisi
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3") // veya daha yenisi
    implementation("androidx.compose.material:material-icons-core:1.7.0") // Bu zaten olmalı
    implementation("androidx.compose.material:material-icons-extended:1.7.0") // BUNU EKLEYİN

    implementation("com.squareup.retrofit2:retrofit:2.9.0") // En son sürümü kontrol edin
    // Gson Converter (JSON parse etmek için)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Veya Moshi, Jackson vb.
    // Opsiyonel: OkHttp Logging Interceptor (Ağ isteklerini loglamak için, geliştirme sırasında çok faydalı)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") // En son sürümü kontrol edin
    implementation("com.patrykandpatrick.vico:compose:1.13.1") // En son sürümü kontrol edin
    implementation("com.patrykandpatrick.vico:compose-m2:1.13.1") // Material 2 theme (veya m3 için compose-m3)
    implementation("com.patrykandpatrick.vico:core:1.13.1")
    implementation("com.patrykandpatrick.vico:views:1.13.1") // Eğer View sisteminde de kullanacaksanız

}