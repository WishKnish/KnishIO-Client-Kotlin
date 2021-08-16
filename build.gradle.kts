import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
  val kotlinVersion = "1.5.10"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  application
}

group = "io.knish"
version = "0.0.1"

repositories {
  mavenCentral()
  maven { url = URI("https://jitpack.io") }
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
  implementation("org.slf4j:slf4j-jdk14:1.7.31")
  implementation("com.google.code.gson:gson:2.8.7")
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnit()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

application {
  mainClassName = "MainKt"
}