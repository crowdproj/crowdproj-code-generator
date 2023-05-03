plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
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
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openapiVersion")
}

gradlePlugin {
    website.set("https://github.com/ysb33r/gradleTest")
    vcsUrl.set("https://github.com/ysb33r/gradleTest.git")
    plugins {
        create("crowdproj-platforms") {
            id = "crowdproj-platforms"
            displayName = "CrowdProj code generation"
            description = "Code generator that generates code for CrowdProj projects in a modular style"
            tags.set(listOf("openapi", "crowdproj", "kotlin", "multiplatform", "modular"))
            implementationClass = "com.crowdproj.plugins.CrowdprojOpenapiGeneratorPlugin"
            version = project.version
        }
    }
}


publishing {
    publications {
        val sj = create<MavenPublication>("maven")
        project.shadow.component(sj)
    }
    repositories {
        mavenLocal()
    }
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
