plugins {
    kotlin("jvm")
//    id("com.github.johnrengelman.shadow")
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
//    `maven-publish`
//    java
//    id("signing")
    id("org.jetbrains.dokka")
//    id("io.codearte.nexus-staging")
}

group = "com.crowdproj.generator"
version = "0.0.6"

repositories {
    mavenCentral()
}

//signing {
//    sign(publishing.publications)
//}

//nexusStaging {
//    serverUrl = "https://s01.oss.sonatype.org/service/local/"
//    packageGroup = "com.crowdproj" //optional if packageGroup == project.getGroup()
////    stagingProfileId = "yourStagingProfileId" //when not defined will be got from server using "packageGroup"
//}

dependencies {
    val kotlinVersion: String by project
    val openapiVersion: String by project

    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation("org.openapitools:openapi-generator-core:$openapiVersion")
    implementation("org.openapitools:openapi-generator:$openapiVersion")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openapiVersion")
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


//publishing {
//    publications {
//        val sj = create<MavenPublication>("maven")
//        project.shadow.component(sj)
//    }
//    repositories {
//        mavenLocal()
//    }
//}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    group = "publishing"
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

//publishing {
//    repositories {
//        val repoHost: String = System.getenv("NEXUS_HOST") ?: "https://maven.pkg.github.com/crowdproj/kotlin-cor"
//        val repoUser: String? = System.getenv("NEXUS_USER") ?: System.getenv("GITHUB_ACTOR")
//        val repoPass: String? = System.getenv("NEXUS_PASS") ?: System.getenv("GITHUB_TOKEN")
//        if (repoUser != null && repoPass != null) {
//            maven {
//                name = "GitHubPackages"
//                url = uri(repoHost)
//                credentials {
//                    username = repoUser
//                    password = repoPass
//                }
//            }
//        }
//
//    }
//    publications {
//        withType(MavenPublication::class).configureEach {
//            artifact(javadocJar)
//            project.shadow.component(this)
//            pom {
//                name.set("CrowdProj code generation")
//                description.set("Code generator that generates code for CrowdProj projects in a modular style")
//                url.set("https://github.com/crowdproj/crowdproj-code-generator")
//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//                developers {
//                    developer {
//                        name.set("Sergey Okatov")
//                        email.set("sokatov@gmail.com")
//                        id.set("svok")
//                        organization.set("CrowdProj")
//                        organizationUrl.set("https://crowdproj.com")
//                        timezone.set("GMT+5")
//                    }
//                }
//                scm {
//                    connection.set("scm:git:git://github.com/crowdproj/kotlin-cor.git")
//                    developerConnection.set("scm:git:ssh://github.com/crowdproj/kotlin-cor.git")
//                    url.set("https://github.com/crowdproj/kotlin-cor")
//                }
//            }
//        }
//    }
//}

tasks {
    withType<Test> {
        useJUnitPlatform()
        reports {
            junitXml.required.set(true)
        }
//        setupTestLogging()
    }

    build {
        println("VERSION!!!: ${project.version} ${project.group}")
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
