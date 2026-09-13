import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.library") version "9.2.0"
  // Not applied. Puts KGP 2.4.10 on the build classpath so AGP's built-in Kotlin compiles with the
  // same compiler the core (kotlin("jvm") 2.4.10) uses; Kotlin metadata is readable at most one minor ahead.
  kotlin("jvm") version "2.4.10" apply false
  id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.knish"
version = "1.1.1"

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

  sourceSets {
    getByName("androidTest") {
      assets.srcDir("../src/test/resources")
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)
  if (project.findProperty("signingInMemoryKey") != null ||
      System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
    signAllPublications()
  }
  coordinates("io.knish", "knishio-client-kotlin-android", version.toString())
  // Empty javadoc jar on purpose: Central requires the artifact to exist; Dokka is not applied
  // in this module, and the boolean ctor's `publishJavadocJar=true` would run the Java javadoc
  // tool over Kotlin sources.
  configure(AndroidSingleVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources(), "release"))
  pom {
    name.set("KnishIO Client Kotlin — Android Keystore")
    description.set("AndroidKeyStore-backed secret storage provider for the KnishIO Kotlin client (TEE / StrongBox custody).")
    url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin")

    licenses {
      license {
        name.set("GNU General Public License v3.0 or later")
        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
      }
    }

    developers {
      developer {
        id.set("wishknish")
        name.set("WishKnish Corp.")
        email.set("dev@wishknish.com")
        organization.set("WishKnish Corp.")
        organizationUrl.set("https://wishknish.com")
      }
      developer {
        id.set("eugene-teplitsky")
        name.set("Eugene Teplitsky")
        organization.set("WishKnish Corp.")
      }
    }

    scm {
      connection.set("scm:git:git://github.com/WishKnish/KnishIO-Client-Kotlin.git")
      developerConnection.set("scm:git:ssh://git@github.com/WishKnish/KnishIO-Client-Kotlin.git")
      url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin")
    }

    issueManagement {
      system.set("GitHub Issues")
      url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin/issues")
    }
  }
}

dependencies {
  // GraalVM polyglot (ML-KEM bridge, lazy) cannot run on ART and cannot be dexed; excluding the direct
  // artifacts drops their whole transitive tree. Everything else in core is plain JVM bytecode <= Java 17.
  api("io.knish:knishio-client-kotlin:1.1.1") {
    exclude(group = "org.graalvm.polyglot")
  }
  implementation("org.bouncycastle:bcprov-jdk18on:1.85")


  testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

tasks.withType<Test>().configureEach {
  systemProperty("knishio.vectors", file("../src/test/resources/cross-platform-test-vectors.json").absolutePath)
}
