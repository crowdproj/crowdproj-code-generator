pluginManagement {
    plugins {
        val serializationVersion: String by settings
        val kotlinVersion: String by settings
        val dokkaVersion: String by settings
        val nexusStagingVersion: String by settings

        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("io.codearte.nexus-staging") version nexusStagingVersion
        kotlin("plugin.serialization") version serializationVersion
    }
}

rootProject.name = "crowdproj-generator"

includeBuild("crowdproj-generator-plugin")
//include("crowdproj-generator-plugin")
include("crowdproj-generator-test")
