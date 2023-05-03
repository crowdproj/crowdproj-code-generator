pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val shadowVersion: String by settings
        val serializationVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
        kotlin("plugin.serialization") version serializationVersion
    }
}

rootProject.name = "crowdproj-code-generator"

//includeBuild("crowdproj-generator-base")
includeBuild("crowdproj-generator-plugin")
//include("crowdproj-generator-plugin")
include("crowdproj-generator-test")
