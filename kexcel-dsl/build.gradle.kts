plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

group = "io.kexcel"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    constraints {
        api(libs.poi.ooxml) {
            because("KExcel requires at least version ${libs.versions.poi.get()} for streaming stability.")
        }
        api(libs.fastexcel) {
            because("KExcel requires at least version ${libs.versions.fastexcel.get()} for memory efficiency.")
        }
    }

    compileOnly(libs.poi.ooxml)
    compileOnly(libs.fastexcel)

    testImplementation(libs.poi.ooxml)
    testImplementation(libs.fastexcel)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar() // Includes source code in the distribution for better developer experience
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "kexcel-dsl"
            version = version.toString()
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
