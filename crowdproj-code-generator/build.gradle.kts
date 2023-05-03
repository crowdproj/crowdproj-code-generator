plugins {
    kotlin("jvm")
//    kotlin("plugin.serialization") version "1.5.0"
}

group = "com.crowdproj.generator"
version = "0.0.4"

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion: String by project
    val openapiVersion: String by project

    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation("org.openapitools:openapi-generator-core:$openapiVersion")
    implementation("org.openapitools:openapi-generator:$openapiVersion")
}
