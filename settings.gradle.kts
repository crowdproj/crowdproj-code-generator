pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val shadowVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
    }
}

rootProject.name = "crowdproj-code-generator"

include("crowdproj-code-generator")
include("crowdproj-generator-plugin")
