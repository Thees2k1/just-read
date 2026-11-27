import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val keystoreProperties = Properties().apply {
  val propsFile = rootProject.file("keystore.properties")
  if (propsFile.exists()) {
    propsFile.inputStream().use { load(it) }
  }
}

fun signingProp(envName: String, propName: String): String? =
  System.getenv(envName)?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(propName)?.takeIf { it.isNotBlank() }

android {
  namespace = "com.justread.app"
  compileSdk {
    version = release(37) {
      minorApiLevel = 0
    }
  }
  defaultConfig {
    applicationId = "com.justread.app"
    minSdk = 24
    targetSdk = 37
    versionCode = (project.findProperty("VERSION_CODE") as String?)?.toIntOrNull()
      ?: System.getenv("VERSION_CODE")?.toIntOrNull()
      ?: 1
    versionName = (project.findProperty("VERSION_NAME") as String?)?.takeIf { it.isNotBlank() }
      ?: System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
      ?: "1.0"
  }

  val releaseStoreFilePath = signingProp("RELEASE_STORE_FILE", "storeFile")
  val releaseStorePassword = signingProp("RELEASE_STORE_PASSWORD", "storePassword")
  val releaseKeyAlias = signingProp("RELEASE_KEY_ALIAS", "keyAlias")
  val releaseKeyPassword = signingProp("RELEASE_KEY_PASSWORD", "keyPassword")
  val hasReleaseSigning =
    releaseStoreFilePath != null &&
      releaseStorePassword != null &&
      releaseKeyAlias != null &&
      releaseKeyPassword != null

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = rootProject.file(releaseStoreFilePath)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    create("profile") {
      initWith(getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
      isMinifyEnabled = true
      isShrinkResources = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
  compilerOptions {
    freeCompilerArgs.addAll(
      "-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
    )
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.kotlinx.serialization.json)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
