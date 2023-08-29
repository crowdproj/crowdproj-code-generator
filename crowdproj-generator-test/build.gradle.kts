plugins {
    kotlin("multiplatform")
//    kotlin("plugin.serialization")
    id("com.crowdproj.generator")
//    java
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

kotlin {
    jvm { withJava() }
    js {
        browser {}
    }
    linuxX64 { }

    sourceSets {
        val serializationVersion: String by project

        val commonMain by getting {

            kotlin.srcDirs("${layout.buildDirectory.get()}/generate-resources/main/src/commonMain/kotlin")
            dependencies {
                implementation(kotlin("stdlib-common"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

crowdprojGenerate {
    inputSpec.set("$projectDir/spec-crowdproj-ad-v1.yaml")
    packageName.set(project.group.toString())
}

afterEvaluate {
    val openApiGenerate = tasks.getByName("openApiGenerate")
    tasks.filter { it.name.startsWith("compile") }.forEach {
        it.dependsOn(openApiGenerate)
    }
    tasks.filter { it.name.endsWith("Elements") }.forEach {
        it.dependsOn(openApiGenerate)
    }
}
