import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.gradle.api.component.AdhocComponentWithVariants

plugins {
  val kotlinVersion = "2.2.0"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("com.gradleup.shadow") version "8.3.6"
  id("org.jetbrains.dokka") version "2.0.0"
  // Maven Central publishing via the Central Portal (replaces the decommissioned
  // OSSRH s01 endpoint). Applies + manages maven-publish and signing internally.
  // 0.36.0+ requires Dokka v2 (it dropped Dokka v1) and Gradle >= 8.13 — both
  // satisfied here (wrapper bumped to 8.13, Dokka on 2.x).
  id("com.vanniktech.maven.publish") version "0.36.0"
  id("jacoco")
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  // CycloneDX SBOM for dependency auditing (CI runs osv-scanner against the BOM;
  // Gradle has no committed lockfile, so the SBOM is the scannable dependency graph).
  id("org.cyclonedx.bom") version "3.2.4"
  `java-library`
}

group = "io.knish"
version = "0.9.2"
description = "KnishIO Client SDK for Kotlin - Post-blockchain distributed ledger technology with quantum-resistant cryptography"

// SBOM for dependency auditing: scope to the SHIPPED graph (runtimeClasspath) so the
// osv-scanner CI gate reflects what consumers install, not build-plugin classpaths.
// (Dokka is on v2 now, which no longer drags the old jackson 2.12 into the plugin
// classpath; the runtimeClasspath scoping remains correct regardless.)
tasks.cyclonedxDirectBom {
  includeConfigs = listOf("runtimeClasspath")
}

repositories {
  // Maven Central only. tweetnacl-java (the sole JitPack artifact) was replaced by
  // the BouncyCastle-backed NaClBox, so jitpack.io + m2.dv8tion.net are gone.
  mavenCentral()
}

dependencies {
  val ktorVersion = "3.5.1"
  val coroutinesVersion = "1.10.2"
  val serializationVersion = "1.9.0"
  val bouncyCastleVersion = "1.85"

  // NaCl crypto_box is implemented on BouncyCastle (libraries/NaClBox.kt), replacing
  // the former JitPack `tweetnacl-java`; byte-parity is pinned by the `nacl`
  // cross-platform vectors (CrossPlatformVectorsTest.naclVectors).
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
  implementation("org.graalvm.polyglot:polyglot:25.1.3")
  implementation("org.graalvm.polyglot:js:25.1.3")
  implementation("org.graalvm.js:js-scriptengine:25.1.3")
  
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
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
  implementation("com.graphql-java:graphql-java:26.0")
  
  // Testing dependencies
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
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

// detekt static-analysis lint gate (cycle 113). Pragmatic baseline: the genuinely-
// valuable rule sets (potential-bugs, empty-blocks, coroutines, exceptions,
// performance) are the enforced gate; the crypto/DLT firehose (complexity,
// MagicNumber, line-length/formatting) is relaxed in config/detekt/detekt.yml —
// mirrors the Rust clippy::all (not pedantic) + Python ruff F (not E501) philosophy.
// Scope = src/main/kotlin minus SelfTest.kt (dev tooling); detekt is read-only static
// analysis (no --auto-correct) so it cannot move the frozen self-test molecular hashes.
detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("config/detekt/detekt.yml"))
  source.setFrom(files("src/main/kotlin"))
  ignoreFailures = false
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  exclude("**/SelfTest.kt")
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
  }
}

// Dokka Gradle plugin v2 (DGP v2) — the `dokka {}` extension replaces the v1
// `tasks.dokkaHtml { … }`; the generation task is now `dokkaGenerate`.
dokka {
  dokkaPublications.html {
    moduleName.set("KnishIO Client Kotlin")
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    // NOTE: do NOT `includes.from("README.md")` — Dokka `includes` expects a
    // module/package-doc file (must start with "# Module"/"# Package"); the
    // README starts with an HTML <div> logo and makes generation fail
    // ("Unexpected classifier: <div"). API docs are generated without it.
  }
  dokkaSourceSets.configureEach {
    sourceLink {
      localDirectory.set(file("src/main/kotlin"))
      remoteUrl("https://github.com/WishKnish/KnishIO-Client-Kotlin/tree/main/src/main/kotlin")
      remoteLineSuffix.set("#L")
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

  // Dokka v2 generation task (was "dokkaHtml" under DGP v1).
  configure(KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))  // sourcesJar defaults to true

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

// Do NOT publish the Shadow fat jar to Maven Central. The `com.gradleup.shadow`
// plugin auto-attaches a `shadowRuntimeElements` variant (the `-all.jar`) to the
// `java` component, and the Vanniktech plugin publishes that whole component — so
// the fat jar was uploaded to Central, where at 0.9.1 it bundled the GraalVM JS
// engine (~87 MB) + BouncyCastle + Ktor and hit ~208 MB, past Sonatype's 80 MB
// limit. A library only needs the thin jar; consumers resolve GraalVM/BouncyCastle/
// Ktor transitively via the POM. `./gradlew shadowJar` still works locally for any
// standalone/CLI use — this only removes the fat jar from the *published* artifact.
(components["java"] as AdhocComponentWithVariants)
  .withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }

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