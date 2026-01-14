plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
}

group = "com.crowdproj.generator"
version = libs.versions.crowdproj.generator.get()

repositories {
    mavenCentral()
}

dependencies {
//    val kotlinVersion: String by project
//    val openapiVersion: String by project
//    val serializationVersion: String by project

//    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation(libs.openapi.main)
    implementation(libs.openapi.core)
    implementation(libs.openapi.plugin)
    implementation(libs.kotlin.serialization)
//    implementation("org.openapitools:openapi-generator-core:$openapiVersion")
//    implementation("org.openapitools:openapi-generator:$openapiVersion")
//    implementation("org.openapitools:openapi-generator-gradle-plugin:$openapiVersion")
//    implementation("org.jetbrains.kotlin:kotlin-serialization:$serializationVersion")
}

gradlePlugin {
    website.set("https://github.com/crowdproj/crowdproj-code-generator")
    vcsUrl.set("https://github.com/crowdproj/crowdproj-code-generator.git")
    plugins {
        register("com.crowdproj.generator") {
            id = "com.crowdproj.generator"
            displayName = "CrowdProj code generation"
            description = "OpenAPI specs based code generator that generates code in a modular style used in CrowdProj projects"
            @Suppress("UnstableApiUsage")
            tags.set(listOf("openapi", "crowdproj", "kotlin", "multiplatform", "modular"))
            implementationClass = "com.crowdproj.plugins.CrowdprojGeneratorPlugin"
            version = project.version
        }
    }
}

val dokkaJavadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    group = "publishing"
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
        }
    }

    publishPlugins {
        dependsOn(build)
    }

    register("deploy") {
        group = "build"
        dependsOn(publishPlugins)
    }

}


kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}
