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

import com.crowdproj.plugins.generator.CwpGeneratorConstants.COLLECTION_TYPE
import com.crowdproj.plugins.generator.CwpGeneratorConstants.DATE_LIBRARY
import com.crowdproj.plugins.generator.CwpGeneratorConstants.MULTIPLATFORM
import com.crowdproj.plugins.generator.CwpGeneratorConstants.REQUEST_DATE_CONVERTER
import com.crowdproj.plugins.generator.CwpGeneratorConstants.VENDOR_EXTENSION_BASE_NAME_LITERAL
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
import java.util.stream.Stream

@Suppress("unused")
class KotlinClientCodegen : AbstractKotlinCodegen() {
    private val LOGGER = LoggerFactory.getLogger(KotlinClientCodegen::class.java)
    var dateLibrary = DateLibrary.STRING.value
    var requestDateConverter = RequestDateConverter.TO_STRING.value
    var collectionType = CollectionType.LIST.value
    var authFolder: String? = null

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

        CWP_CLI_OPTIONS.firstOrNull { it.opt == CodegenConstants.LIBRARY }?.enum?.also { supportedLibraries.putAll(it) }
        setLibrary(MULTIPLATFORM)
        cliOptions.addAll(CWP_CLI_OPTIONS)

    }

    @Suppress("unused")
    override fun getTag(): CodegenType = CodegenType.SERVER

    @Suppress("unused")
    override fun getName(): String = "kotlin-crowdproj"

    @Suppress("unused")
    override fun getHelp(): String = "Generates a Kotlin Crowdproj code"

//    private fun setDateLibrary(library: String) {
//        dateLibrary = library
//    }
//
//    private fun setRequestDateConverter(converter: String) {
//        requestDateConverter = converter
//    }
//
//    private fun setCollectionType(collectionType: String) {
//        this.collectionType = collectionType
//    }

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
            }
            additionalProperties[CodegenConstants.SOURCE_FOLDER] = this.sourceFolder
        }
        super.processOpts()

        // infrastructure destination folder
        val folderPrefix = "$sourceFolder${File.separator}${packageName.replace(".", "/")}${File.separator}"
        val infrastructureFolder = "${folderPrefix}infrastructure"
        authFolder = "${folderPrefix}auth"

        // additional properties
        if (additionalProperties.containsKey(DATE_LIBRARY)) {
            dateLibrary = additionalProperties[DATE_LIBRARY].toString()
        }
        if (additionalProperties.containsKey(REQUEST_DATE_CONVERTER)) {
            requestDateConverter = additionalProperties[REQUEST_DATE_CONVERTER].toString()
        }
        commonSupportingFiles()
        when (getLibrary()) {
            MULTIPLATFORM -> processMultiplatformLibrary(infrastructureFolder)
            else -> {}
        }
        processDateLibrary()
        processRequestDateConverter()
        if (additionalProperties.containsKey(COLLECTION_TYPE)) {
            collectionType = additionalProperties[COLLECTION_TYPE].toString()
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
        additionalProperties[DateLibrary.THREETENBP.value] = true
        typeMapping["date"] = "LocalDate"
        importMapping["LocalDate"] = "org.threeten.bp.LocalDate"
        defaultIncludes.add("org.threeten.bp.LocalDate")
        if (dateLibrary == DateLibrary.THREETENBP.value) {
            typeMapping["date-time"] = "org.threeten.bp.OffsetDateTime"
            typeMapping["DateTime"] = "OffsetDateTime"
            importMapping["OffsetDateTime"] = "org.threeten.bp.OffsetDateTime"
            defaultIncludes.add("org.threeten.bp.OffsetDateTime")
        } else if (dateLibrary == DateLibrary.THREETENBP_LOCALDATETIME.value) {
            typeMapping["date-time"] = "org.threeten.bp.LocalDateTime"
            typeMapping["DateTime"] = "LocalDateTime"
            importMapping["LocalDateTime"] = "org.threeten.bp.LocalDateTime"
            defaultIncludes.add("org.threeten.bp.LocalDateTime")
        }
    }

    private fun processStringDate() {
        typeMapping["date-time"] = "kotlin.String"
        typeMapping["date"] = "kotlin.String"
        typeMapping["Date"] = "kotlin.String"
        typeMapping["DateTime"] = "kotlin.String"
    }

    private fun processJava8Date(dateLibrary: String) {
        additionalProperties[DateLibrary.JAVA8.value] = true
        if (dateLibrary == DateLibrary.JAVA8.value) {
            typeMapping["date-time"] = "java.time.OffsetDateTime"
            typeMapping["DateTime"] = "OffsetDateTime"
            importMapping["OffsetDateTime"] = "java.time.OffsetDateTime"
        } else if (dateLibrary == DateLibrary.JAVA8_LOCALDATETIME.value) {
            typeMapping["date-time"] = "java.time.LocalDateTime"
            typeMapping["DateTime"] = "LocalDateTime"
            importMapping["LocalDateTime"] = "java.time.LocalDateTime"
        }
    }

    private fun addSupportingSerializerAdapters(infrastructureFolder: String) {
        supportingFiles.add(
            SupportingFile(
                "jvm-common/infrastructure/Serializer.kt.mustache",
                infrastructureFolder,
                "Serializer.kt"
            )
        )
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

//    private fun processJVMKtorLibrary(infrastructureFolder: String) {
//        // in future kotlinx.serialization may be added
//        if (this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.gson && this.serializationLibrary != SERIALIZATION_LIBRARY_TYPE.jackson) {
//            this.serializationLibrary =
//                SERIALIZATION_LIBRARY_TYPE.jackson
//        }
//        additionalProperties.put(JVM, true)
//        additionalProperties.put(JVM_KTOR, true)
//        defaultIncludes.add("io.ktor.client.request.forms.InputProvider")
//        importMapping.put("InputProvider", "io.ktor.client.request.forms.InputProvider")
//        supportingFiles.add(
//            SupportingFile(
//                "infrastructure/ApiAbstractions.kt.mustache",
//                infrastructureFolder,
//                "ApiAbstractions.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "infrastructure/ApiClient.kt.mustache",
//                infrastructureFolder,
//                "ApiClient.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "infrastructure/HttpResponse.kt.mustache",
//                infrastructureFolder,
//                "HttpResponse.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "infrastructure/RequestConfig.kt.mustache",
//                infrastructureFolder,
//                "RequestConfig.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "infrastructure/RequestMethod.kt.mustache",
//                infrastructureFolder,
//                "RequestMethod.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "auth/ApiKeyAuth.kt.mustache",
//                authFolder,
//                "ApiKeyAuth.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "auth/Authentication.kt.mustache",
//                authFolder,
//                "Authentication.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "auth/HttpBasicAuth.kt.mustache",
//                authFolder,
//                "HttpBasicAuth.kt"
//            )
//        )
//        supportingFiles.add(
//            SupportingFile(
//                "auth/HttpBearerAuth.kt.mustache",
//                authFolder,
//                "HttpBearerAuth.kt"
//            )
//        )
//        supportingFiles.add(SupportingFile("auth/OAuth.kt.mustache", authFolder, "OAuth.kt"))
//    }

    private fun processMultiplatformLibrary(infrastructureFolder: String) {
        commonJvmMultiplatformSupportingFiles(infrastructureFolder)
        additionalProperties.put(MULTIPLATFORM, true)
        dateLibrary = DateLibrary.STRING.value
        requestDateConverter = RequestDateConverter.TO_STRING.value

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
        supportingFiles.addAll(
            listOf(
                SupportingFile("README.mustache", "", "README.md"),
                SupportingFile("build.gradle.kts.mustache", "", "build.gradle.kts"),
                SupportingFile("settings.gradle.kts.mustache", "", "settings.gradle.kts"),

                // gradle wrapper supporting files
                SupportingFile("gradlew.mustache", "", "gradlew"),
                SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"),
                SupportingFile(
                    "gradle-wrapper.properties.mustache",
                    "gradle.wrapper".replace(".", File.separator),
                    "gradle-wrapper.properties"
                ),
                SupportingFile(
                    "gradle-wrapper.jar",
                    "gradle.wrapper".replace(".", File.separator),
                    "gradle-wrapper.jar"
                ),
            )
        )
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): MutableMap<String, ModelsMap> {
        val ready = super.postProcessAllModels(objs)
        for ((idx, modelsMap) in ready) {
            val model = modelsMap?.models?.firstOrNull()?.model ?: continue
            val parentName = model.parent ?: continue
            val parent = ready[parentName]?.models?.firstOrNull()?.model ?: continue
            val mappings = parent.discriminator?.mappedModels
            val mappingModel = mappings?.firstOrNull { it.modelName == idx }
            val mappingName = mappingModel?.mappingName
            if (!mappingName.isNullOrBlank()) {
                model.vendorExtensions["x-mappingName"] = mappingName
            }
            (model.vars + model.allVars + model.requiredVars).forEach { v ->
                if (v.baseName == parent.discriminatorName) {
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
            for (vr in vars) {
                vr?.vendorExtensions?.put(VENDOR_EXTENSION_BASE_NAME_LITERAL, vr.baseName.replace("$", "\\$"))
            }
        }
        return objects
    }

    override fun postProcessOperationsWithModels(
        objs: OperationsMap,
        allModels: List<ModelMap>
    ): OperationsMap {
        super.postProcessOperationsWithModels(objs, allModels)
        val operations: OperationMap = objs.operations
        val ops: List<CodegenOperation> = operations.operation
        for (operation in ops) {
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
            if (MULTIPLATFORM == getLibrary() && operation.allParams != null) {
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
        private fun isMultipartType(consumes: List<Map<String, String>>): Boolean {
            val firstType = consumes[0]
            return "multipart/form-data" == firstType["mediaType"]
        }
    }
}
