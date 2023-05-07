plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
}

group = "com.crowdproj.generator"
version = "0.0.6"

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion: String by project
    val openapiVersion: String by project

    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation("org.openapitools:openapi-generator-core:$openapiVersion")
    implementation("org.openapitools:openapi-generator:$openapiVersion")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openapiVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.8.21")
}

gradlePlugin {
    @Suppress("UnstableApiUsage")
    website.set("https://github.com/crowdproj/crowdproj-code-generator")
    @Suppress("UnstableApiUsage")
    vcsUrl.set("https://github.com/crowdproj/crowdproj-code-generator.git")
    plugins {
        create("com.crowdproj.generator") {
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

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    group = "publishing"
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
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

    create("deploy") {
        group = "build"
        dependsOn(publishPlugins)
    }

}


kotlin {
    jvmToolchain(17)
}
