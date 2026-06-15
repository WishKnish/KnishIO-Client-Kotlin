import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
  val kotlinVersion = "2.2.0"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("com.gradleup.shadow") version "8.3.6"
  id("org.jetbrains.dokka") version "1.9.20"
  // Maven Central publishing via the Central Portal (replaces the decommissioned
  // OSSRH s01 endpoint). Applies + manages maven-publish and signing internally.
  // Pinned to 0.34.0: the last line that supports this repo's Gradle 8.11.1
  // (0.35.0+ require Gradle 8.13) AND Dokka v1 / 1.9.20 (0.36.0 dropped Dokka v1).
  id("com.vanniktech.maven.publish") version "0.34.0"
  id("jacoco")
  `java-library`
}

group = "io.knish"
version = "0.8.1"
description = "KnishIO Client SDK for Kotlin - Post-blockchain distributed ledger technology with quantum-resistant cryptography"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://m2.dv8tion.net/releases")
}

dependencies {
  val ktorVersion = "3.2.0"
  val coroutinesVersion = "1.10.2"
  val serializationVersion = "1.9.0"
  val bouncyCastleVersion = "1.80"

  implementation("io.github.instantwebp2p:tweetnacl-java:1.1.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  
  // BouncyCastle with post-quantum cryptography support
  implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
  implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
  implementation("org.bouncycastle:bcutil-jdk18on:$bouncyCastleVersion")
  
  // NOTE: asia.hombre:kyber (KyberKotlin) removed — it had ZERO references in
  // src/. ML-KEM is provided by the GraalJS noble-ml-kem bridge below (kept) for
  // byte-identical cross-SDK parity with the JS SDK.

  // GraalJS for JavaScript interop (noble-post-quantum bridge — LOAD-BEARING:
  // Wallet.preparePostQuantumKeys + encrypt/decrypt route ML-KEM through the
  // bundled noble-ml-kem-bundle.js for JS-SDK-identical keys. Do NOT remove.)
  implementation("org.graalvm.polyglot:polyglot:24.0.2")
  implementation("org.graalvm.polyglot:js:24.0.2")
  implementation("org.graalvm.js:js-scriptengine:24.0.2")
  
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-logging:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
  implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  // kotlinx-coroutines-android removed — Android dispatcher artifact, unused in a JVM SDK.
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
  // slf4j-jdk14 removed — a library must not ship an SLF4J *binding* (forces JUL on
  // consumers); no slf4j usage in main (Ktor uses its own Logger).
  implementation("com.google.code.gson:gson:2.14.0")
  implementation("com.graphql-java:graphql-java:22.3")
  
  // Testing dependencies
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("io.mockk:mockk:1.14.3")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
  testImplementation("io.strikt:strikt-core:0.35.1")
}

kotlin {
  jvmToolchain(17)
}

// NOTE: sources + javadoc jars are produced by the Vanniktech plugin's
// configure(KotlinJvm(...)) below — do NOT also call java.withSourcesJar()/
// withJavadocJar() here (duplicate-artifact conflict).

// Unit test suite. Mirrors the other SDKs' shape: two shared cross-SDK vector
// tests (PatentVectorValidationTest over canonical-patent-vectors.json,
// CrossPlatformVectorsTest over cross-platform-test-vectors.json) plus
// per-concern unit tests. No bespoke generators/integration scaffolding.
tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)

  // Eliminate runtime warnings that mask real SDK issues
  jvmArgs(
    "--enable-native-access=ALL-UNNAMED",  // Fix JANSI native access warnings
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",  // Fix NIO warnings
    "--add-opens=java.base/java.io=ALL-UNNAMED"      // Fix IO warnings
  )
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }
}

jacoco {
  toolVersion = "0.8.11"
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
  }
}

tasks.dokkaHtml {
  outputDirectory.set(layout.buildDirectory.dir("dokka"))
  dokkaSourceSets {
    configureEach {
      moduleName.set("KnishIO Client Kotlin")
      // NOTE: do NOT `includes.from("README.md")` — Dokka `includes` expects a
      // module/package-doc file (must start with "# Module"/"# Package"); the
      // README starts with an HTML <div> logo and makes dokkaHtml fail
      // ("Unexpected classifier: <div"). API docs are generated without it.
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/WishKnish/KnishIO-Client-Kotlin/tree/main/src/main/kotlin").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

// Maven Central publishing via the Sonatype Central Portal (the OSSRH s01
// endpoint was decommissioned 2025-06-30). The Vanniktech plugin produces the
// sources + Dokka-javadoc jars, signs all publications, and uploads+releases in
// a single `publishToMavenCentral` task. Credentials come from the
// ORG_GRADLE_PROJECT_mavenCentralUsername/Password + signingInMemoryKey* env
// vars in CI (see .github/workflows/ci.yml); absent locally, signing is skipped
// so `publishToMavenLocal` still works.
mavenPublishing {
  publishToMavenCentral(automaticRelease = true)

  // Sign only when a key is configured (CI: the ORG_GRADLE_PROJECT_signingInMemoryKey
  // secret). Locally there's no key, so skip signing — otherwise publishToMavenLocal
  // fails with "no configured signatory". Maven Central REQUIRES signatures, so the
  // CI publish (which sets the secret) always signs.
  if (project.findProperty("signingInMemoryKey") != null ||
      System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
    signAllPublications()
  }

  // Explicit, lowercase artifactId (otherwise it defaults to rootProject.name
  // "KnishIO-Client-Kotlin"); matches the README coords io.knish:knishio-client-kotlin.
  coordinates("io.knish", "knishio-client-kotlin", version.toString())

  configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaHtml")))  // sourcesJar defaults to true

  pom {
    name.set("KnishIO Client Kotlin")
    description.set(project.description)
    url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin")

    licenses {
      license {
        // The LICENSE file is GPL v3; all sibling SDKs are GPL-3.0-or-later.
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

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

// Task to run the SDK self-test
tasks.register<JavaExec>("selftest") {
  description = "Run the SDK self-test to validate core functionality against reference values"
  group = "verification"
  
  // Only compile main sources, skip tests
  dependsOn(tasks.compileKotlin, tasks.processResources, tasks.classes)
  
  // Set working directory to project root for file access
  workingDir = projectDir
  
  // Run the self-test class
  mainClass.set("SelfTestKt")
  classpath = sourceSets["main"].runtimeClasspath
  
  // Clean runtime environment - eliminate external library warnings
  jvmArgs(
    "--enable-native-access=ALL-UNNAMED",  // Fix JANSI native access warnings
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",  // Fix NIO warnings
    "--add-opens=java.base/java.io=ALL-UNNAMED"      // Fix IO warnings
  )
}