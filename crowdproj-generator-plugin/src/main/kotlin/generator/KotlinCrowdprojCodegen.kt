/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crowdproj.plugins.generator

import org.apache.commons.lang3.StringUtils
import org.openapitools.codegen.*
import org.openapitools.codegen.languages.AbstractKotlinCodegen
import org.openapitools.codegen.meta.FeatureSet
import org.openapitools.codegen.meta.features.*
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationMap
import org.openapitools.codegen.model.OperationsMap
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.function.Predicate
import java.util.stream.Stream

@Suppress("unused")
class KotlinClientCodegen : AbstractKotlinCodegen() {
    private val LOGGER = LoggerFactory.getLogger(KotlinClientCodegen::class.java)
    private var dateLibrary = DateLibrary.JAVA8.value
    private var requestDateConverter = RequestDateConverter.TO_JSON.value
    private var collectionType = CollectionType.LIST.value

    private var authFolder: String? = null

    enum class DateLibrary(val value: String) {
        STRING("string"),
        THREETENBP("threetenbp"),
        THREETENBP_LOCALDATETIME("threetenbp-localdatetime"),
        JAVA8("java8"),
        JAVA8_LOCALDATETIME("java8-localdatetime")
    }

    enum class RequestDateConverter(val value: String) {
        TO_STRING("toString"),
        TO_JSON("toJson")
    }

    enum class CollectionType(val value: String) {
        ARRAY("array"),
        LIST("list")
    }

    /**
     * Constructs an instance of `KotlinClientCodegen`.
     */
    init {

        /*
         * OAuth flows supported _only_ by client explicitly setting bearer token. The "flows" are not supported.
         */
        modifyFeatureSet { features: FeatureSet.Builder ->
            features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .excludeWireFormatFeatures(
                    WireFormatFeature.XML,
                    WireFormatFeature.PROTOBUF
                )
                .excludeSecurityFeatures(
                    SecurityFeature.OpenIDConnect,
                    SecurityFeature.OAuth2_Password,
                    SecurityFeature.OAuth2_AuthorizationCode,
                    SecurityFeature.OAuth2_ClientCredentials,
                    SecurityFeature.OAuth2_Implicit
                )
                .excludeGlobalFeatures(
                    GlobalFeature.XMLStructureDefinitions,
                    GlobalFeature.Callbacks,
                    GlobalFeature.LinkObjects,
                    GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                    SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                    ParameterFeature.Cookie
                )
                .includeClientModificationFeatures(ClientModificationFeature.BasePath)
        }
        artifactId = "kotlin-crowdproj"
        packageName = "org.openapitools.client"

        // cliOptions default redefinition need to be updated
        updateOption(CodegenConstants.ARTIFACT_ID, this.artifactId)
        updateOption(CodegenConstants.PACKAGE_NAME, this.packageName)
        outputFolder = "generated-code${File.separator}kotlin-crowdproj"
        modelTemplateFiles["model.mustache"] = ".kt"
        apiTemplateFiles["api.mustache"] = ".kt"
        modelDocTemplateFiles["model_doc.mustache"] = ".md"
        apiDocTemplateFiles["api_doc.mustache"] = ".md"
        templateDir = "kotlin-crowdproj"
        embeddedTemplateDir = templateDir
        apiPackage = "$packageName.apis"
        modelPackage = "$packageName.models"
        val dateLibrary = CliOption(DATE_LIBRARY, "Option. Date library to use")
        val dateOptions: MutableMap<String, String> = HashMap()
        dateOptions[DateLibrary.THREETENBP.value] =
            "Threetenbp - Backport of JSR310 (jvm only, preferred for jdk < 1.8)"
        dateOptions[DateLibrary.THREETENBP_LOCALDATETIME.value] =
            "Threetenbp - Backport of JSR310 (jvm only, for legacy app only)"
        dateOptions[DateLibrary.STRING.value] = "String"
        dateOptions[DateLibrary.JAVA8.value] = "Java 8 native JSR310 (jvm only, preferred for jdk 1.8+)"
        dateOptions[DateLibrary.JAVA8_LOCALDATETIME.value] = "Java 8 native JSR310 (jvm only, for legacy app only)"
        dateLibrary.enum = dateOptions
        dateLibrary.default = this.dateLibrary
        cliOptions.add(dateLibrary)
        val collectionType = CliOption(COLLECTION_TYPE, "Option. Collection type to use")
        val collectionOptions: Map<String, String> = mapOf(
            CollectionType.ARRAY.value to "kotlin.Array",
            CollectionType.LIST.value to "kotlin.collections.List",
        )
        collectionType.enum = collectionOptions
        collectionType.default = this.collectionType
        cliOptions.add(collectionType)
        supportedLibraries.putAll(
            mapOf(
                JVM_KTOR to
                        "Platform: Java Virtual Machine. HTTP client: Ktor 1.6.7. JSON processing: Gson, Jackson (default).",
                JVM_OKHTTP4 to
                        "[DEFAULT] Platform: Java Virtual Machine. HTTP client: OkHttp 4.2.0 (Android 5.0+ and Java 8+). JSON processing: Moshi 1.8.0.",
                JVM_OKHTTP3 to
                        "Platform: Java Virtual Machine. HTTP client: OkHttp 3.12.4 (Android 2.3+ and Java 7+). JSON processing: Moshi 1.8.0.",
                JVM_RETROFIT2 to "Platform: Java Virtual Machine. HTTP client: Retrofit 2.6.2.",
                MULTIPLATFORM to
                        "Platform: Kotlin multiplatform. HTTP client: Ktor 1.6.7. JSON processing: Kotlinx Serialization: 1.2.1.",
                JVM_VOLLEY to
                        "Platform: JVM for Android. HTTP client: Volley 1.2.1. JSON processing: gson 2.8.9",
                JVM_VERTX to
                        "Platform: Java Virtual Machine. HTTP client: Vert.x Web Client. JSON processing: Moshi, Gson or Jackson.",
            )
        )
        val libraryOption = CliOption(
            CodegenConstants.LIBRARY,
            "Library template (sub-template) to use"
        )
        libraryOption.enum = supportedLibraries
        libraryOption.default = JVM_OKHTTP4
        cliOptions.add(libraryOption)
        setLibrary(JVM_OKHTTP4)
        val requestDateConverter = CliOption(
            REQUEST_DATE_CONVERTER,
            "JVM-Option. Defines in how to handle date-time objects that are used for a request (as query or parameter)"
        )
        val requestDateConverterOptions: MutableMap<String, String> = HashMap()
        requestDateConverterOptions[RequestDateConverter.TO_JSON.value] =
            "[DEFAULT] Date formatter option using a json converter."
        requestDateConverterOptions[RequestDateConverter.TO_STRING.value] =
            "Use the 'toString'-method of the date-time object to retrieve the related string representation."
        requestDateConverter.enum = requestDateConverterOptions
        requestDateConverter.default = this.requestDateConverter
        cliOptions.addAll(
            listOf(
                requestDateConverter,
                CliOption.newBoolean(
                    OMIT_GRADLE_PLUGIN_VERSIONS,
                    "Whether to declare Gradle plugin versions in build files."
                ),
                CliOption.newBoolean(
                    OMIT_GRADLE_WRAPPER,
                    "Whether to omit Gradle wrapper for creating a sub project."
                ),
                CliOption.newBoolean(
                    USE_SETTINGS_GRADLE,
                    "Whether the project uses settings.gradle."
                ),
                CliOption.newBoolean(
                    IDEA,
                    "Add IntellJ Idea plugin and mark Kotlin main and test folders as source folders."
                ),
                CliOption.newBoolean(
                    MOSHI_CODE_GEN,
                    "Whether to enable codegen with the Moshi library. Refer to the [official Moshi doc](https://github.com/square/moshi#codegen) for more info."
                ),
                CliOption.newBoolean(
                    SUPPORT_ANDROID_API_LEVEL_25_AND_BELLOW,
                    "[WARNING] This flag will generate code that has a known security vulnerability. It uses `kotlin.io.createTempFile` instead of `java.nio.file.Files.createTempFile` in order to support Android API level 25 and bellow. For more info, please check the following links https://github.com/OpenAPITools/openapi-generator/security/advisories/GHSA-23x4-m842-fmwf, https://github.com/OpenAPITools/openapi-generator/pull/9284"
                ),
            )
        )
    }

    @Suppress("unused")
    override fun getTag(): CodegenType = CodegenType.SERVER

    @Suppress("unused")
    override fun getName(): String = "kotlin-crowdproj"

    @Suppress("unused")
    override fun getHelp(): String = "Generates a Kotlin Crowdproj code"

    fun setDateLibrary(library: String) {
        dateLibrary = library
    }

    fun setRequestDateConverter(converter: String) {
        requestDateConverter = converter
    }

    fun setCollectionType(collectionType: String) {
        this.collectionType = collectionType
    }

    override fun modelFilename(templateName: String, modelName: String): String {
        val suffix: String =
            modelTemplateFiles().get(templateName) ?: throw RuntimeException("modelTemplateFiles is null")
        // If this was a proper template method, i wouldn't have to make myself throw up by doing this....
        return modelFileFolder() + File.separator + toModelFilename(modelName) + suffix
    }

    override fun processOpts() {
        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            setSourceFolder(additionalProperties[CodegenConstants.SOURCE_FOLDER] as String)
        } else {
            // Set the value to defaults if we haven't overridden
            if (MULTIPLATFORM == getLibrary()) {
                setSourceFolder("src/commonMain/kotlin")
            } else if (JVM_VOLLEY == getLibrary()) {
                // Android plugin wants it's source in java
                setSourceFolder("src/main/java")
            } else {
                setSourceFolder(super.sourceFolder)
            }
            additionalProperties[CodegenConstants.SOURCE_FOLDER] = this.sourceFolder
        }
        super.processOpts()

        // infrastructure destination folder
        val infrastructureFolder: String =
            (sourceFolder + File.separator + packageName + File.separator + "infrastructure").replace(".", "/")
        authFolder = (sourceFolder + File.separator + packageName + File.separator + "auth").replace(".", "/")

        // request destination folder
        val requestFolder: String =
            (sourceFolder + File.separator + packageName + File.separator + "request").replace(".", "/")

        // auth destination folder
        val authFolder: String =
            (sourceFolder + File.separator + packageName + File.separator + "auth").replace(".", "/")

        // additional properties
        if (additionalProperties.containsKey(DATE_LIBRARY)) {
            setDateLibrary(additionalProperties.get(DATE_LIBRARY).toString())
        }
        if (additionalProperties.containsKey(REQUEST_DATE_CONVERTER)) {
            setRequestDateConverter(additionalProperties.get(REQUEST_DATE_CONVERTER).toString())
        }
        commonSupportingFiles()
        when (getLibrary()) {
            JVM_KTOR -> processJVMKtorLibrary(infrastructureFolder)
            JVM_OKHTTP3, JVM_OKHTTP4 -> processJVMOkHttpLibrary(infrastructureFolder)
            JVM_VOLLEY -> processJVMVolleyLibrary(infrastructureFolder, requestFolder)
            JVM_RETROFIT2 -> processJVMRetrofit2Library(infrastructureFolder)
            MULTIPLATFORM -> processMultiplatformLibrary(infrastructureFolder)
            JVM_VERTX -> processJVMVertXLibrary(infrastructureFolder)
            else -> {}
        }
        processDateLibrary()
        processRequestDateConverter()
        if (additionalProperties.containsKey(COLLECTION_TYPE)) {
            setCollectionType(additionalProperties.get(COLLECTION_TYPE).toString())
        }
        if (CollectionType.LIST.value == collectionType) {
            if (isModelMutable) {
                typeMapping.put("array", "kotlin.collections.MutableList")
                typeMapping.put("list", "kotlin.collections.MutableList")
            } else {
                typeMapping.put("array", "kotlin.collections.List")
                typeMapping.put("list", "kotlin.collections.List")
            }
            additionalProperties.put("isList", true)
        }
        if (usesRetrofit2Library()) {
            val hasOAuthMethods: Boolean = org.openapitools.codegen.utils.ProcessUtils.hasOAuthMethods(openAPI)
            if (hasOAuthMethods) {
                supportingFiles.add(
                    SupportingFile(
                        "auth/OAuth.kt.mustache",
                        authFolder,
                        "OAuth.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "auth/OAuthFlow.kt.mustache",
                        authFolder,
                        "OAuthFlow.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "auth/OAuthOkHttpClient.kt.mustache",
                        authFolder,
                        "OAuthOkHttpClient.kt"
                    )
                )
            }
            if (hasOAuthMethods || org.openapitools.codegen.utils.ProcessUtils.hasApiKeyMethods(openAPI)) {
                supportingFiles.add(
                    SupportingFile(
                        "auth/ApiKeyAuth.kt.mustache",
                        authFolder,
                        "ApiKeyAuth.kt"
                    )
                )
            }
            if (org.openapitools.codegen.utils.ProcessUtils.hasHttpBearerMethods(openAPI)) {
                supportingFiles.add(
                    SupportingFile(
                        "auth/HttpBearerAuth.kt.mustache",
                        authFolder,
                        "HttpBearerAuth.kt"
                    )
                )
            }
            if (org.openapitools.codegen.utils.ProcessUtils.hasHttpBasicMethods(openAPI)) {
                supportingFiles.add(
                    SupportingFile(
                        "auth/HttpBasicAuth.kt.mustache",
                        authFolder,
                        "HttpBasicAuth.kt"
                    )
                )
            }
        }
    }

    private fun processDateLibrary() {
        if (DateLibrary.THREETENBP.value == dateLibrary || DateLibrary.THREETENBP_LOCALDATETIME.value == dateLibrary) {
            processThreeTenBpDate(dateLibrary)
        } else if (DateLibrary.STRING.value == dateLibrary) {
            processStringDate()
        } else if (DateLibrary.JAVA8.value == dateLibrary || DateLibrary.JAVA8_LOCALDATETIME.value == dateLibrary) {
            processJava8Date(dateLibrary)
        }
    }

    private fun processRequestDateConverter() {
        if (RequestDateConverter.TO_JSON.value == requestDateConverter) {
            additionalProperties.put(RequestDateConverter.TO_JSON.value, true)
        } else if (RequestDateConverter.TO_STRING.value == requestDateConverter) {
            additionalProperties.put(RequestDateConverter.TO_STRING.value, true)
        }
    }

    private fun processThreeTenBpDate(dateLibrary: String) {
        additionalProperties.put(DateLibrary.THREETENBP.value, true)
        typeMapping.put("date", "LocalDate")
        importMapping.put("LocalDate", "org.threeten.bp.LocalDate")
        defaultIncludes.add("org.threeten.bp.LocalDate")
        if (dateLibrary == DateLibrary.THREETENBP.value) {
            typeMapping.put("date-time", "org.threeten.bp.OffsetDateTime")
            typeMapping.put("DateTime", "OffsetDateTime")
            importMapping.put("OffsetDateTime", "org.threeten.bp.OffsetDateTime")
            defaultIncludes.add("org.threeten.bp.OffsetDateTime")
        } else if (dateLibrary == DateLibrary.THREETENBP_LOCALDATETIME.value) {
            typeMapping.put("date-time", "org.threeten.bp.LocalDateTime")
            typeMapping.put("DateTime", "LocalDateTime")
            importMapping.put("LocalDateTime", "org.threeten.bp.LocalDateTime")
            defaultIncludes.add("org.threeten.bp.LocalDateTime")
        }
    }

    private fun processStringDate() {
        typeMapping.put("date-time", "kotlin.String")
        typeMapping.put("date", "kotlin.String")
        typeMapping.put("Date", "kotlin.String")
        typeMapping.put("DateTime", "kotlin.String")
    }

    private fun processJava8Date(dateLibrary: String) {
        additionalProperties.put(DateLibrary.JAVA8.value, true)
        if (dateLibrary == DateLibrary.JAVA8.value) {
            typeMapping.put("date-time", "java.time.OffsetDateTime")
            typeMapping.put("DateTime", "OffsetDateTime")
            importMapping.put("OffsetDateTime", "java.time.OffsetDateTime")
        } else if (dateLibrary == DateLibrary.JAVA8_LOCALDATETIME.value) {
            typeMapping.put("date-time", "java.time.LocalDateTime")
            typeMapping.put("DateTime", "LocalDateTime")
            importMapping.put("LocalDateTime", "java.time.LocalDateTime")
        }
    }

    private fun processJVMRetrofit2Library(infrastructureFolder: String) {
        additionalProperties.put(JVM, true)
        additionalProperties.put(JVM_RETROFIT2, true)
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiClient.kt.mustache",
                infrastructureFolder,
                "ApiClient.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ResponseExt.kt.mustache",
                infrastructureFolder,
                "ResponseExt.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/CollectionFormats.kt.mustache",
                infrastructureFolder,
                "CollectionFormats.kt"
            )
        )
        addSupportingSerializerAdapters(infrastructureFolder)
    }

    private fun processJVMVolleyLibrary(infrastructureFolder: String, requestFolder: String) {
        additionalProperties.put(JVM, true)
        additionalProperties.put(JVM_VOLLEY, true)
        supportingFiles.add(
            SupportingFile(
                "infrastructure/CollectionFormats.kt.mustache",
                infrastructureFolder,
                "CollectionFormats.kt"
            )
        )

        // We have auth related partial files, so they can be overridden, but don't generate them explicitly
        supportingFiles.add(
            SupportingFile(
                "request/GsonRequest.mustache",
                requestFolder,
                "GsonRequest.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "request/IRequestFactory.mustache",
                requestFolder,
                "IRequestFactory.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "request/RequestFactory.mustache",
                requestFolder,
                "RequestFactory.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/CollectionFormats.kt.mustache",
                infrastructureFolder,
                "CollectionFormats.kt"
            )
        )
        if (getSerializationLibrary() != SERIALIZATION_LIBRARY_TYPE.gson) {
            throw RuntimeException("This library currently only supports gson serialization. Try adding '--additional-properties serializationLibrary=gson' to your command.")
        }
        addSupportingSerializerAdapters(infrastructureFolder)
        supportingFiles.remove(
            SupportingFile(
                "jvm-common/infrastructure/Serializer.kt.mustache",
                infrastructureFolder,
                "Serializer.kt"
            )
        )
    }

    private fun addSupportingSerializerAdapters(infrastructureFolder: String) {
        supportingFiles.add(
            SupportingFile(
                "jvm-common/infrastructure/Serializer.kt.mustache",
                infrastructureFolder,
                "Serializer.kt"
            )
        )
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (getSerializationLibrary()) {
            SERIALIZATION_LIBRARY_TYPE.moshi -> {
                if (enumUnknownDefaultCase) {
                    supportingFiles.add(
                        SupportingFile(
                            "jvm-common/infrastructure/SerializerHelper.kt.mustache",
                            infrastructureFolder,
                            "SerializerHelper.kt"
                        )
                    )
                }
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/ByteArrayAdapter.kt.mustache",
                        infrastructureFolder,
                        "ByteArrayAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/UUIDAdapter.kt.mustache",
                        infrastructureFolder,
                        "UUIDAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateTimeAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "OffsetDateTimeAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/BigDecimalAdapter.kt.mustache",
                        infrastructureFolder,
                        "BigDecimalAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/BigIntegerAdapter.kt.mustache",
                        infrastructureFolder,
                        "BigIntegerAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/URIAdapter.kt.mustache",
                        infrastructureFolder,
                        "URIAdapter.kt"
                    )
                )
            }

            SERIALIZATION_LIBRARY_TYPE.gson -> {
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/ByteArrayAdapter.kt.mustache",
                        infrastructureFolder,
                        "ByteArrayAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateTimeAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "OffsetDateTimeAdapter.kt"
                    )
                )
            }

            SERIALIZATION_LIBRARY_TYPE.jackson -> {}
            SERIALIZATION_LIBRARY_TYPE.kotlinx_serialization -> {
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/AtomicBooleanAdapter.kt.mustache",
                        infrastructureFolder,
                        "AtomicBooleanAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/AtomicIntegerAdapter.kt.mustache",
                        infrastructureFolder,
                        "AtomicIntegerAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/AtomicLongAdapter.kt.mustache",
                        infrastructureFolder,
                        "AtomicLongAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/URIAdapter.kt.mustache",
                        infrastructureFolder,
                        "URIAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/URLAdapter.kt.mustache",
                        infrastructureFolder,
                        "URLAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/BigIntegerAdapter.kt.mustache",
                        infrastructureFolder,
                        "BigIntegerAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/BigDecimalAdapter.kt.mustache",
                        infrastructureFolder,
                        "BigDecimalAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/LocalDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "LocalDateTimeAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/OffsetDateTimeAdapter.kt.mustache",
                        infrastructureFolder,
                        "OffsetDateTimeAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/UUIDAdapter.kt.mustache",
                        infrastructureFolder,
                        "UUIDAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/StringBuilderAdapter.kt.mustache",
                        infrastructureFolder,
                        "StringBuilderAdapter.kt"
                    )
                )
                supportingFiles.add(
                    SupportingFile(
                        "jvm-common/infrastructure/proguard-rules.pro.mustache",
                        "",
                        "proguard-rules.pro"
                    )
                )
            }
        }
    }

    private fun processJVMKtorLibrary(infrastructureFolder: String) {
        // in future kotlinx.serialization may be added
        if (this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.gson && this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.jackson) {
            this.serializationLibrary =
                SERIALIZATION_LIBRARY_TYPE.jackson
        }
        additionalProperties.put(JVM, true)
        additionalProperties.put(JVM_KTOR, true)
        defaultIncludes.add("io.ktor.client.request.forms.InputProvider")
        importMapping.put("InputProvider", "io.ktor.client.request.forms.InputProvider")
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiAbstractions.kt.mustache",
                infrastructureFolder,
                "ApiAbstractions.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiClient.kt.mustache",
                infrastructureFolder,
                "ApiClient.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/HttpResponse.kt.mustache",
                infrastructureFolder,
                "HttpResponse.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/RequestConfig.kt.mustache",
                infrastructureFolder,
                "RequestConfig.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/RequestMethod.kt.mustache",
                infrastructureFolder,
                "RequestMethod.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/ApiKeyAuth.kt.mustache",
                authFolder,
                "ApiKeyAuth.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/Authentication.kt.mustache",
                authFolder,
                "Authentication.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/HttpBasicAuth.kt.mustache",
                authFolder,
                "HttpBasicAuth.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/HttpBearerAuth.kt.mustache",
                authFolder,
                "HttpBearerAuth.kt"
            )
        )
        supportingFiles.add(SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"))
    }

    /**
     * Process Vert.x client options
     *
     * @param infrastructureFolder infrastructure destination folder
     */
    private fun processJVMVertXLibrary(infrastructureFolder: String) {
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiAbstractions.kt.mustache",
                infrastructureFolder,
                "ApiAbstractions.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiClient.kt.mustache",
                infrastructureFolder,
                "ApiClient.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/Errors.kt.mustache",
                infrastructureFolder,
                "Errors.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiResponse.kt.mustache",
                infrastructureFolder,
                "ApiResponse.kt"
            )
        )
        addSupportingSerializerAdapters(infrastructureFolder)
        additionalProperties.put(JVM, true)
        additionalProperties.put(JVM_VERTX, true)
    }

    private fun processJVMOkHttpLibrary(infrastructureFolder: String) {
        commonJvmMultiplatformSupportingFiles(infrastructureFolder)
        addSupportingSerializerAdapters(infrastructureFolder)
        additionalProperties.put(JVM, true)
        additionalProperties.put(JVM_OKHTTP, true)
        if (JVM_OKHTTP4 == getLibrary()) {
            additionalProperties.put(JVM_OKHTTP4, true)
        } else if (JVM_OKHTTP3 == getLibrary()) {
            additionalProperties.put(JVM_OKHTTP3, true)
        }
        supportedLibraries.put(
            JVM_OKHTTP,
            "A workaround to use the same template folder for both 'jvm-okhttp3' and 'jvm-okhttp4'."
        )
        setLibrary(JVM_OKHTTP)

        // jvm specific supporting files
        supportingFiles.add(
            SupportingFile(
                "infrastructure/Errors.kt.mustache",
                infrastructureFolder,
                "Errors.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ResponseExtensions.kt.mustache",
                infrastructureFolder,
                "ResponseExtensions.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiResponse.kt.mustache",
                infrastructureFolder,
                "ApiResponse.kt"
            )
        )
    }

    private fun processMultiplatformLibrary(infrastructureFolder: String) {
        commonJvmMultiplatformSupportingFiles(infrastructureFolder)
        additionalProperties.put(MULTIPLATFORM, true)
        setDateLibrary(DateLibrary.STRING.value)
        setRequestDateConverter(RequestDateConverter.TO_STRING.value)

        // multiplatform default includes
        defaultIncludes.add("io.ktor.client.request.forms.InputProvider")
        defaultIncludes.add(packageName + ".infrastructure.Base64ByteArray")
        defaultIncludes.add(packageName + ".infrastructure.OctetByteArray")

        // multiplatform type mapping
        typeMapping.put("number", "kotlin.Double")
        typeMapping.put("file", "OctetByteArray")
        typeMapping.put("binary", "OctetByteArray")
        typeMapping.put("ByteArray", "Base64ByteArray")
        typeMapping.put("object", "kotlin.String") // kotlin.Any not serializable

        // multiplatform import mapping
        importMapping.put("BigDecimal", "kotlin.Double")
        importMapping.put("UUID", "kotlin.String")
        importMapping.put("URI", "kotlin.String")
        importMapping.put("InputProvider", "io.ktor.client.request.forms.InputProvider")
        importMapping.put("File", packageName + ".infrastructure.OctetByteArray")
        importMapping.put("Timestamp", "kotlin.String")
        importMapping.put("LocalDateTime", "kotlin.String")
        importMapping.put("LocalDate", "kotlin.String")
        importMapping.put("LocalTime", "kotlin.String")
        importMapping.put("Base64ByteArray", packageName + ".infrastructure.Base64ByteArray")
        importMapping.put("OctetByteArray", packageName + ".infrastructure.OctetByteArray")

        // multiplatform specific supporting files
        supportingFiles.add(
            SupportingFile(
                "infrastructure/Base64ByteArray.kt.mustache",
                infrastructureFolder,
                "Base64ByteArray.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/Bytes.kt.mustache",
                infrastructureFolder,
                "Bytes.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/HttpResponse.kt.mustache",
                infrastructureFolder,
                "HttpResponse.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/OctetByteArray.kt.mustache",
                infrastructureFolder,
                "OctetByteArray.kt"
            )
        )

        // multiplatform specific auth
        supportingFiles.add(
            SupportingFile(
                "auth/ApiKeyAuth.kt.mustache",
                authFolder,
                "ApiKeyAuth.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/Authentication.kt.mustache",
                authFolder,
                "Authentication.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/HttpBasicAuth.kt.mustache",
                authFolder,
                "HttpBasicAuth.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "auth/HttpBearerAuth.kt.mustache",
                authFolder,
                "HttpBearerAuth.kt"
            )
        )
        supportingFiles.add(SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"))

        // multiplatform specific testing files
        supportingFiles.add(
            SupportingFile(
                "commonTest/Coroutine.kt.mustache",
                "src/commonTest/kotlin/util",
                "Coroutine.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "iosTest/Coroutine.kt.mustache",
                "src/iosTest/kotlin/util",
                "Coroutine.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "jsTest/Coroutine.kt.mustache",
                "src/jsTest/kotlin/util",
                "Coroutine.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "jvmTest/Coroutine.kt.mustache",
                "src/jvmTest/kotlin/util",
                "Coroutine.kt"
            )
        )
    }

    private fun commonJvmMultiplatformSupportingFiles(infrastructureFolder: String) {
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiClient.kt.mustache",
                infrastructureFolder,
                "ApiClient.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/ApiAbstractions.kt.mustache",
                infrastructureFolder,
                "ApiAbstractions.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/PartConfig.kt.mustache",
                infrastructureFolder,
                "PartConfig.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/RequestConfig.kt.mustache",
                infrastructureFolder,
                "RequestConfig.kt"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "infrastructure/RequestMethod.kt.mustache",
                infrastructureFolder,
                "RequestMethod.kt"
            )
        )
    }

    private fun commonSupportingFiles() {
        supportingFiles.add(SupportingFile("README.mustache", "", "README.md"))
        if (getLibrary() == MULTIPLATFORM) {
            supportingFiles.add(
                SupportingFile(
                    "build.gradle.kts.mustache",
                    "",
                    "build.gradle.kts"
                )
            )
            supportingFiles.add(
                SupportingFile(
                    "settings.gradle.kts.mustache",
                    "",
                    "settings.gradle.kts"
                )
            )
        } else if (getLibrary() == JVM_VOLLEY) {
            supportingFiles.add(SupportingFile("build.mustache", "", "build.gradle"))
            supportingFiles.add(
                SupportingFile(
                    "gradle.properties.mustache",
                    "",
                    "gradle.properties"
                )
            )
            supportingFiles.add(
                SupportingFile(
                    "settings.gradle.mustache",
                    "",
                    "settings.gradle"
                )
            )
            supportingFiles.add(
                SupportingFile(
                    "manifest.mustache",
                    "",
                    "src/main/AndroidManifest.xml"
                )
            )
        } else {
            supportingFiles.add(SupportingFile("build.gradle.mustache", "", "build.gradle"))
            supportingFiles.add(
                SupportingFile(
                    "settings.gradle.mustache",
                    "",
                    "settings.gradle"
                )
            )
        }

        // gradle wrapper supporting files
        supportingFiles.add(SupportingFile("gradlew.mustache", "", "gradlew"))
        supportingFiles.add(SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"))
        supportingFiles.add(
            SupportingFile(
                "gradle-wrapper.properties.mustache",
                "gradle.wrapper".replace(".", File.separator),
                "gradle-wrapper.properties"
            )
        )
        supportingFiles.add(
            SupportingFile(
                "gradle-wrapper.jar",
                "gradle.wrapper".replace(".", File.separator),
                "gradle-wrapper.jar"
            )
        )
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): MutableMap<String, ModelsMap> {
        val ready = super.postProcessAllModels(objs)
        for((idx, modelsMap) in ready) {
            val model = modelsMap?.models?.firstOrNull()?.model ?: continue
            val parentName = model.parent ?: continue
            val parent = ready[parentName]?.models?.firstOrNull()?.model ?: continue
            val mappings = parent.discriminator?.mappedModels
            val mappingModel = mappings?.firstOrNull { it.modelName == idx }
            val mappingName = mappingModel?.mappingName
            if (!mappingName.isNullOrBlank()) {
                model.vendorExtensions["x-mappingName"] = mappingName
                LOGGER.error("postProcessAllModels: $idx ${model.name} - $parentName - ${parent.name} - $mappings - $mappingName")
            }
            (model.vars + model.allVars + model.requiredVars).forEach { v ->
                if(v.baseName == parent.discriminatorName) {
                    v.isDiscriminator = true
                }
            }
        }
        return ready
    }

    override fun postProcessModels(objs: ModelsMap): ModelsMap {
        val objects: ModelsMap = super.postProcessModels(objs)
        for (mo in objects.models) {
            val cm: CodegenModel = mo.model

            // escape the variable base name for use as a string literal
            val vars: List<CodegenProperty?> =
                Stream.of<List<CodegenProperty?>>(
                    cm.vars,
                    cm.allVars,
                    cm.optionalVars,
                    cm.requiredVars,
                    cm.readOnlyVars,
                    cm.readWriteVars,
                    cm.parentVars
                )
                    .flatMap<CodegenProperty?> { obj: List<CodegenProperty?> -> obj.stream() }
                    .toList()
            for (`var` in vars) {
                `var`?.vendorExtensions?.put(VENDOR_EXTENSION_BASE_NAME_LITERAL, `var`.baseName.replace("$", "\\$"))
            }
        }
        return objects
    }

    private fun usesRetrofit2Library(): Boolean {
        return getLibrary() != null && getLibrary().contains(JVM_RETROFIT2)
    }

    override fun postProcessOperationsWithModels(
        objs: OperationsMap,
        allModels: List<ModelMap>
    ): OperationsMap {
        super.postProcessOperationsWithModels(objs, allModels)
        LOGGER.error("postProcessOperationsWithModels: ${allModels.size}")
        val operations: OperationMap = objs.operations
        val ops: List<CodegenOperation> = operations.operation
        for (operation in ops) {
            if (JVM_RETROFIT2 == getLibrary() && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith(
                    "/"
                )
            ) {
                operation.path = operation.path.substring(1)
            }
            if (JVM_OKHTTP == getLibrary() || JVM_OKHTTP3 == getLibrary() || JVM_OKHTTP4 == getLibrary()) {
                // Ideally we would do content negotiation to choose the best mediatype, but that would be a next step.
                // For now we take the first mediatype we can parse and send that.
                val isSerializable =
                    Predicate { typeMapping: Map<String?, String?> ->
                        val mediaTypeValue = typeMapping["mediaType"] ?: return@Predicate false
                        // match on first part in mediaTypes like 'application/json; charset=utf-8'
                        val endIndex = mediaTypeValue.indexOf(';')
                        val mediaType =
                            (if (endIndex == -1) mediaTypeValue else mediaTypeValue.substring(
                                0,
                                endIndex
                            )).trim { it <= ' ' }
                        "multipart/form-data" == mediaType || "application/x-www-form-urlencoded" == mediaType || mediaType.startsWith(
                            "application/"
                        ) && mediaType.endsWith("json")
                    }
                operation.consumes = if (operation.consumes == null) null else operation.consumes.stream()
                    .filter(isSerializable)
                    .limit(1)
                    .toList()
//                        .collect<List<Map<String, String>>, Any>(Collectors.toList<Map<String, String>>())
                operation.hasConsumes = operation.consumes != null && !operation.consumes.isEmpty()
                operation.produces = if (operation.produces == null) null else operation.produces.stream()
                    .filter(isSerializable)
                    .toList()
//                        .collect<List<Map<String, String>>, Any>(Collectors.toList<Map<String, String>>())
                operation.hasProduces = operation.produces != null && !operation.produces.isEmpty()
            }

            // set multipart against all relevant operations
            if (operation.hasConsumes == java.lang.Boolean.TRUE) {
                if (isMultipartType(operation.consumes)) {
                    operation.isMultipart = java.lang.Boolean.TRUE
                }
            }

            // import okhttp3.MultipartBody if any parameter is a file
            for (param in operation.allParams) {
                if (java.lang.Boolean.TRUE == param.isFile) {
                    operations.put("x-kotlin-multipart-import", true)
                }
            }
            if (usesRetrofit2Library() && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith(
                    "/"
                )
            ) {
                operation.path = operation.path.substring(1)
            }

            // sorting operation parameters to make sure path params are parsed before query params
            if (operation.allParams != null) {
                Collections.sort(operation.allParams,
                    Comparator { one: CodegenParameter, another: CodegenParameter ->
                        if (one.isPathParam && another.isQueryParam) {
                            return@Comparator -1
                        }
                        if (one.isQueryParam && another.isPathParam) {
                            return@Comparator 1
                        }
                        0
                    })
            }

            // modify the data type of binary form parameters to a more friendly type for ktor builds
            if ((JVM_KTOR == getLibrary() || MULTIPLATFORM == getLibrary()) && operation.allParams != null) {
                for (param in operation.allParams) {
                    if (param.dataFormat != null && param.dataFormat == "binary") {
                        param.dataType = "io.ktor.client.request.forms.InputProvider"
                        param.baseType = param.dataType
                    }
                }
            }
        }
        return objs
    }

//    override fun postProcess() {
//        println("################################################################################")
//        println("# Thanks for using OpenAPI Generator.                                          #")
//        println("# Please consider donation to help us maintain this project \uD83D\uDE4F                 #")
//        println("# https://opencollective.com/openapi_generator/donate                          #")
//        println("#                                                                              #")
//        println("# This generator's contributed by Jim Schubert (https://github.com/jimschubert)#")
//        println("# Please support his work directly via https://patreon.com/jimschubert \uD83D\uDE4F      #")
//        println("################################################################################")
//    }

    companion object {
        private const val JVM = "jvm"
        private const val JVM_KTOR = "jvm-ktor"
        private const val JVM_OKHTTP = "jvm-okhttp"
        private const val JVM_OKHTTP4 = "jvm-okhttp4"
        private const val JVM_OKHTTP3 = "jvm-okhttp3"
        private const val JVM_RETROFIT2 = "jvm-retrofit2"
        private const val MULTIPLATFORM = "multiplatform"
        private const val JVM_VOLLEY = "jvm-volley"
        private const val JVM_VERTX = "jvm-vertx"
        const val OMIT_GRADLE_PLUGIN_VERSIONS = "omitGradlePluginVersions"
        const val OMIT_GRADLE_WRAPPER = "omitGradleWrapper"
        const val USE_SETTINGS_GRADLE = "useSettingsGradle"
        const val IDEA = "idea"
        const val DATE_LIBRARY = "dateLibrary"
        const val REQUEST_DATE_CONVERTER = "requestDateConverter"
        const val COLLECTION_TYPE = "collectionType"
        const val MOSHI_CODE_GEN = "moshiCodeGen"
        const val SUPPORT_ANDROID_API_LEVEL_25_AND_BELLOW = "supportAndroidApiLevel25AndBelow"
        private const val VENDOR_EXTENSION_BASE_NAME_LITERAL = "x-base-name-literal"
        private fun isMultipartType(consumes: List<Map<String, String>>): Boolean {
            val firstType = consumes[0]
            return "multipart/form-data" == firstType["mediaType"]
        }
    }
}
