group = "com.crowdproj.generator"
version = libs.versions.crowdproj.generator.get()

repositories {
    mavenCentral()
}

tasks {
    register("deploy") {
        dependsOn(gradle.includedBuild("crowdproj-generator-plugin").task(":deploy"))
    }
}
