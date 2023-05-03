pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        val shadowVersion: String by settings
        val dokkaVersion: String by settings
        val nexusStagingVersion: String by settings
        val pluginPublishVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("io.codearte.nexus-staging") version nexusStagingVersion
        id("com.gradle.plugin-publish") version pluginPublishVersion
    }
}

rootProject.name = "crowdproj-generator-plugin"
