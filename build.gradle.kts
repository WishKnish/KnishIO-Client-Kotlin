import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
  val kotlinVersion = "2.2.0"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.jetbrains.dokka") version "1.9.20"
  id("maven-publish")
  id("signing")
  id("jacoco")
  `java-library`
}

group = "io.knish"
version = "1.0.0-RC1"
description = "KnishIO Client SDK for Kotlin - Post-blockchain distributed ledger technology with quantum-resistant cryptography"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://m2.dv8tion.net/releases")
}

dependencies {
  val ktorVersion = "2.3.12"
  val coroutinesVersion = "1.8.1"
  val serializationVersion = "1.7.3"
  val bouncyCastleVersion = "1.79"

  implementation("io.github.instantwebp2p:tweetnacl-java:1.1.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  
  // BouncyCastle with post-quantum cryptography support
  implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
  implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
  implementation("org.bouncycastle:bcutil-jdk18on:$bouncyCastleVersion")
  
  // KyberKotlin for ML-KEM implementation (NIST FIPS-203 compliant)
  implementation("asia.hombre:kyber:2.0.0")
  
  // GraalJS for JavaScript interop (noble-post-quantum bridge)
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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
  implementation("org.slf4j:slf4j-jdk14:2.0.16")
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.graphql-java:graphql-java:22.3")
  
  // Testing dependencies
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("io.mockk:mockk:1.13.13")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
  testImplementation("io.strikt:strikt-core:0.34.1")
}

kotlin {
  jvmToolchain(17)
}

java {
  withJavadocJar()
  withSourcesJar()
}

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
      includes.from("README.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/WishKnish/KnishIO-Client-Kotlin/tree/main/src/main/kotlin").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      
      artifact(dokkaJavadocJar)
      
      pom {
        name.set("KnishIO Client Kotlin")
        description.set("KnishIO Client SDK for Kotlin - Post-blockchain distributed ledger technology with quantum-resistant cryptography")
        url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin")
        
        licenses {
          license {
            name.set("MIT License")
            url.set("https://opensource.org/licenses/MIT")
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
          developerConnection.set("scm:git:ssh://github.com:WishKnish/KnishIO-Client-Kotlin.git")
          url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin")
        }
        
        issueManagement {
          system.set("GitHub Issues")
          url.set("https://github.com/WishKnish/KnishIO-Client-Kotlin/issues")
        }
      }
    }
  }
  
  repositories {
    maven {
      name = "sonatype"
      val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
      val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
      url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
      
      credentials {
        username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
        password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
      }
    }
    
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/WishKnish/KnishIO-Client-Kotlin")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
        password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
      }
    }
  }
}

signing {
  val signingKeyId = project.findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
  val signingKey = project.findProperty("signing.secretKeyRingFile") as String? ?: System.getenv("SIGNING_KEY")
  val signingPassword = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
  
  if (signingKeyId != null && signingKey != null && signingPassword != null) {
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["maven"])
  }
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

// Task to run the unified test vector validator
tasks.register<JavaExec>("validateTestVectors") {
  description = "Run the unified test vector validator using SDK methods"
  group = "verification"
  
  classpath = sourceSets["test"].runtimeClasspath
  mainClass.set("knishio.test.RunValidator")
  
  // Set working directory to project root for file access
  workingDir = projectDir
  
  // Clean runtime environment - eliminate external library warnings
  jvmArgs(
    "--enable-native-access=ALL-UNNAMED",  // Fix JANSI native access warnings
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",  // Fix NIO warnings
    "--add-opens=java.base/java.io=ALL-UNNAMED"      // Fix IO warnings
  )
}