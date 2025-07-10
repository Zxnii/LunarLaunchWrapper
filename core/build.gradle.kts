dependencies {
    api(rootProject.libs.semver)
    
    implementation(rootProject.libs.arrow.core)
    implementation(rootProject.libs.arrow.coroutines)
    
    implementation(rootProject.libs.commons.lang3)
    
    implementation(rootProject.libs.kx.coroutines.core)
    implementation(rootProject.libs.kx.datetime)
    
    implementation(rootProject.libs.kx.ser.core)
    implementation(rootProject.libs.kx.ser.json)
    
    implementation(rootProject.libs.ktor.client.core)
    implementation(rootProject.libs.ktor.client.okhttp)
    implementation(rootProject.libs.ktor.client.negotiation)
    implementation(rootProject.libs.ktor.serialization.kxser.json)
    
    implementation(rootProject.libs.asm.tree)
    implementation(rootProject.libs.asm.util)
    implementation(rootProject.libs.asm.commons)
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.12")
}