package com.crowdproj.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.buildscript

@Suppress("unused")
class CrowdprojGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.buildscript {
            dependencies {
                classpath("org.openapitools:openapi-generator:6.5.0")
            }
        }
        project.pluginManager.apply("org.openapi.generator")
//        val extension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    }
}
