plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kexcel-dsl"))
    
    // Engines for benchmarking
    implementation(libs.poi.ooxml)
    implementation(libs.fastexcel)
    
    // JMH annotation processing
    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

jmh {
    // JMH Options
    warmupIterations = 3
    iterations = 5
    fork = 1
    
    // Profilers and JVM args
    profilers = listOf("gc")
    jvmArgs = listOf("-Xmx512m", "-XX:+UseG1GC")
    
    // Result format for visualization
    resultFormat = "JSON"
    
    // Output file
    resultsFile = project.layout.buildDirectory.file("reports/jmh/results.json")
}

kotlin {
    jvmToolchain(21)
}
