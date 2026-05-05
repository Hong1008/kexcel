plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kexcel-dsl"))
    implementation(libs.poi.ooxml)
    implementation("org.slf4j:slf4j-simple:2.0.12")
    
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
