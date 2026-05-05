plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kexcel"
include("kexcel-dsl")
include("kexcel-dsl-example:poi")
include("kexcel-dsl-example:fastexcel")
