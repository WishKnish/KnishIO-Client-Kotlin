pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "knishio-client-kotlin-android"

includeBuild("..") {
  dependencySubstitution {
    substitute(module("io.knish:knishio-client-kotlin")).using(project(":"))
  }
}
