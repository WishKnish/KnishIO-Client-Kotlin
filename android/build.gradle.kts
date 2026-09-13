import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.library") version "9.2.0"
  // Not applied. Puts KGP 2.4.10 on the build classpath so AGP's built-in Kotlin compiles with the
  // same compiler the core (kotlin("jvm") 2.4.10) uses; Kotlin metadata is readable at most one minor ahead.
  kotlin("jvm") version "2.4.10" apply false
}

group = "io.knish"
version = "1.1.0"

android {
  namespace = "wishKnish.knishIO.client.storage.keystore"
  compileSdk = 36

  defaultConfig {
    minSdk = 31
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  packaging {
    resources {
      excludes += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

dependencies {
  // GraalVM polyglot (ML-KEM bridge, lazy) cannot run on ART and cannot be dexed; excluding the direct
  // artifacts drops their whole transitive tree. Everything else in core is plain JVM bytecode <= Java 17.
  api("io.knish:knishio-client-kotlin:1.1.0") {
    exclude(group = "org.graalvm.polyglot")
  }
  implementation("org.bouncycastle:bcprov-jdk18on:1.85")


  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
}
