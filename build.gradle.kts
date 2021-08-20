import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
  val kotlinVersion = "1.5.10"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("com.github.johnrengelman.shadow") version "6.1.0"
  `java-library`
  application
}

group = "io.knish"
version = "0.0.1"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://m2.dv8tion.net/releases")
  jcenter { url = URI("https://jcenter.bintray.com") }
}

dependencies {
  val ktorVersion = "1.6.1"

  implementation("io.github.instantwebp2p:tweetnacl-java:1.1.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
  implementation("org.bouncycastle:bcpkix-jdk15on:1.69")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-logging:$ktorVersion")
  implementation("io.ktor:ktor-client-serialization:$ktorVersion")
  implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.5.1")
  implementation("org.slf4j:slf4j-jdk14:1.7.31")
  implementation("com.google.code.gson:gson:2.8.7")
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnit()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}


application {
  mainClass.set("MainKt")
  @Suppress("DEPRECATION")
  mainClassName = mainClass.toString()
}
