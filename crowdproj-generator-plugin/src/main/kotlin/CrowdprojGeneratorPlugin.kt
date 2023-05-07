package com.crowdproj.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.repositories
import org.openapitools.generator.gradle.plugin.extensions.OpenApiGeneratorGenerateExtension

@Suppress("unused")
class CrowdprojGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) = project.run {
        buildscript {
            repositories {
                maven { url = uri("https://plugins.gradle.org/m2/") }
            }
            dependencies {
                classpath("org.openapitools:openapi-generator:6.5.0")
                classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.21")
            }
        }
        pluginManager.apply("org.openapi.generator")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        val cwpExtension = extensions.create<CrowdprojGeneratorPluginExtension>("crowdprojGenerate")
        afterEvaluate {
            val oaExtension = project.extensions.getByType(OpenApiGeneratorGenerateExtension::class.java)
            val pckgName = cwpExtension.packageName.get()
            oaExtension.apply {
                generatorName.set("kotlin-crowdproj")
                packageName.set(pckgName)
                apiPackage.set("$pckgName.api")
                modelPackage.set("$pckgName.models")
                invokerPackage.set("$pckgName.invoker")
                inputSpec.set(cwpExtension.inputSpec.get())
                library.set("multiplatform")
//    templateDir.set("$projectDir/templates")

                globalProperties.set(
                    mapOf(
                        //        "debugModels" to "true",
                        "models" to "",
                        "modelDocs" to "false",
                    )
                )

                configOptions.set(
                    mapOf(
                        "enumPropertyNaming" to "UPPERCASE",
                    )
                )
            }
        }
    }
}
