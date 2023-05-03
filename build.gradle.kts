group = "com.crowdproj.generator"
version = "0.0.4"

repositories {
    mavenCentral()
}

tasks {
    create("deploy") {
        dependsOn(gradle.includedBuild("crowdproj-generator-plugin").task(":deploy"))
    }
}
