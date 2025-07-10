plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    kotlin("jvm") version "2.0.10"
    kotlin("plugin.serialization") version "2.0.10" apply false
}

allprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("org.jetbrains.kotlin.plugin.serialization")

    group = "wtf.zani.llw"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://jogamp.org/deployment/maven")
    }

    kotlin {
        jvmToolchain(17)
    }
}