import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

// Secrets live in local.properties (git ignored). A blank key still builds; generation then fails with the 401 message.
val localProps = Properties().apply {
  val file = rootProject.file("local.properties")
  if (file.exists()) file.inputStream().use { load(it) }
}

android {
  namespace = "com.dimenzy.app"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.dimenzy.app"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

    buildConfigField("String", "STABILITY_API_KEY", "\"${localProps.getProperty("STABILITY_API_KEY", "")}\"")
  }

  buildTypes {
    release {
      // R8 can break Filament and Retrofit without keep rules; spec 0001 leaves it off.
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  buildFeatures {
    compose = true
    buildConfig = true
    aidl = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.androidx.navigation.compose)

  // 3D preview and ARCore placement (includes the 3D only sceneview module).
  implementation(libs.sceneview.ar)
}
