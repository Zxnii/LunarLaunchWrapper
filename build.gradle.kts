plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"

    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
}

group = "wtf.zani.launchwrapper"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    val asmVersion = "9.4"
    val ktorVersion = "2.3.3"

    implementation("org.ow2.asm:asm-tree:$asmVersion")
    implementation("org.ow2.asm:asm-util:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")

    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}

kotlin {
    jvmToolchain(17)
}

task("run", type = JavaExec::class) {
    mainClass.set("wtf.zani.launchwrapper.LunarLaunchWrapperKt")

    args = listOf("--version", "1.8.9", "--module", "lunar")
    jvmArgs = listOf("--add-opens", "java.base/java.io=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED")
    classpath = sourceSets.main.get().runtimeClasspath
}
