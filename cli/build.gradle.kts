plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":core"))
    
    implementation(rootProject.libs.kx.cli)
    implementation(rootProject.libs.kx.coroutines.core)

    implementation(rootProject.libs.arrow.core)
    implementation(rootProject.libs.arrow.coroutines)
}

task("run", type = JavaExec::class) {
    mainClass.set("wtf.zani.llw.cli.MainKt")

    args = listOf("--version", "1.8.9", "--module", "lunar")
    jvmArgs = listOf("--add-opens", "java.base/java.io=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED")
    classpath = sourceSets.main.get().runtimeClasspath
}