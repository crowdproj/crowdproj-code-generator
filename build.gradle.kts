group = "com.crowdproj.generator"
version = "0.1.0"

repositories {
    mavenCentral()
}

tasks {
    create("deploy") {
        dependsOn(gradle.includedBuild("crowdproj-generator-plugin").task(":deploy"))
    }
}
