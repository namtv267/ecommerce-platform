plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    apply(plugin = "java")

    group = "com.shop"
    version = "0.1.0"

    repositories { mavenCentral() }

    configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    tasks.withType<JavaCompile> { options.compilerArgs.add("-parameters") }
    tasks.withType<Test> { useJUnitPlatform() }
}
