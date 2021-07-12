import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.0"
    application
}

group = "io.knish"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url=URI("https://jitpack.io") }
}

dependencies {
    implementation("io.github.instantwebp2p:tweetnacl-java:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.69")
    implementation("io.ktor:ktor-client-core:1.6.1")
    implementation("io.ktor:ktor-client-cio:1.6.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}