package com.crowdproj.plugins.generator

import com.crowdproj.plugins.generator.CwpGeneratorConstants.COLLECTION_TYPE
import com.crowdproj.plugins.generator.CwpGeneratorConstants.DATE_LIBRARY
import com.crowdproj.plugins.generator.CwpGeneratorConstants.MULTIPLATFORM
import org.openapitools.codegen.CliOption
import org.openapitools.codegen.CodegenConstants

val CWP_CLI_OPTIONS = setOf(
    CliOption(DATE_LIBRARY, "Option. Date library to use").apply {
        enum = mapOf(
            DateLibrary.THREETENBP.value to "Threetenbp - Backport of JSR310 (jvm only, preferred for jdk < 1.8)",
            DateLibrary.THREETENBP_LOCALDATETIME.value to "Threetenbp - Backport of JSR310 (jvm only, for legacy app only)",
            DateLibrary.STRING.value to "String",
            DateLibrary.JAVA8.value to "Java 8 native JSR310 (jvm only, preferred for jdk 1.8+)",
            DateLibrary.JAVA8_LOCALDATETIME.value to "Java 8 native JSR310 (jvm only, for legacy app only)",
        )
        this.default = DateLibrary.STRING.value
    },

    CliOption(COLLECTION_TYPE, "Option. Collection type to use").apply {
        enum = mapOf(
            CollectionType.ARRAY.value to "kotlin.Array",
            CollectionType.LIST.value to "kotlin.collections.List",
        )
        default = CollectionType.LIST.value
    },

    CliOption(CodegenConstants.LIBRARY, "Library template (sub-template) to use").apply {
        enum = mapOf(
            MULTIPLATFORM to
                    "Platform: Kotlin multiplatform. HTTP client: Ktor 1.6.7. JSON processing: Kotlinx Serialization: 1.2.1.",
        )
        default = MULTIPLATFORM
    }

//                CliOption.newBoolean(
//                    OMIT_GRADLE_PLUGIN_VERSIONS,
//                    "Whether to declare Gradle plugin versions in build files."
//                ),
//                CliOption.newBoolean(
//                    OMIT_GRADLE_WRAPPER,
//                    "Whether to omit Gradle wrapper for creating a sub project."
//                ),
//                CliOption.newBoolean(
//                    USE_SETTINGS_GRADLE,
//                    "Whether the project uses settings.gradle."
//                ),
//                CliOption.newBoolean(
//                    IDEA,
//                    "Add IntellJ Idea plugin and mark Kotlin main and test folders as source folders."
//                ),
//                CliOption.newBoolean(
//                    MOSHI_CODE_GEN,
//                    "Whether to enable codegen with the Moshi library. Refer to the [official Moshi doc](https://github.com/square/moshi#codegen) for more info."
//                ),
//                CliOption.newBoolean(
//                    SUPPORT_ANDROID_API_LEVEL_25_AND_BELLOW,
//                    "[WARNING] This flag will generate code that has a known security vulnerability. It uses `kotlin.io.createTempFile` instead of `java.nio.file.Files.createTempFile` in order to support Android API level 25 and bellow. For more info, please check the following links https://github.com/OpenAPITools/openapi-generator/security/advisories/GHSA-23x4-m842-fmwf, https://github.com/OpenAPITools/openapi-generator/pull/9284"
//                ),

)
