import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
  val kotlinVersion = "1.9.10"

  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("com.github.johnrengelman.shadow") version "8.1.1"
  `java-library`
  `maven-publish`
  application
}

group = "io.knish"
version = "0.0.1"

repositories {
  mavenCentral()
  maven {
    url = URI("https://jitpack.io")
  }
  maven {
    url = URI("https://m2.dv8tion.net/releases")
  }
  maven {
    url = URI("https://jcenter.bintray.com")
  }
  maven {
    url = URI("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
  }
}

dependencies {
  val ktorVersion = "2.3.5"
  val coroutinesVersion = "1.7.3"

  implementation("io.github.instantwebp2p:tweetnacl-java:1.1.2")
  implementation(kotlin("reflect"))
  implementation("org.bouncycastle:bcpkix-jdk15on:1.69")
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-logging:$ktorVersion")
  implementation("io.ktor:ktor-client-serialization:$ktorVersion")
  implementation("io.ktor:ktor-client-logging:$ktorVersion")
  implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
  implementation("org.slf4j:slf4j-jdk14:2.0.9")
  implementation("com.google.code.gson:gson:2.8.9")
  implementation("com.graphql-java:graphql-java:21.1")
  testImplementation(kotlin("test"))
}


tasks.test {
  useJUnit()
}

tasks.withType<KotlinCompile> {

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
    freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
  }
}

buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("com.github.johnrengelman:shadow:8.1.1")
  }
}

apply(plugin = "com.github.johnrengelman.shadow")


application {
  mainClass.set("MainKt")
}
