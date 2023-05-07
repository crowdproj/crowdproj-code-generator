package com.crowdproj.plugins

import org.gradle.api.provider.Property

interface CrowdprojGeneratorPluginExtension {
    val packageName: Property<String>
    val inputSpec: Property<String>
}
