group = "com.crowdproj.generator"
version = "0.0.5"

repositories {
    mavenCentral()
}

tasks {
    create("deploy") {
        dependsOn(gradle.includedBuild("crowdproj-generator-plugin").task(":deploy"))
    }
}
