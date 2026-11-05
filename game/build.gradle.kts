import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The game rules live in a Kotlin Multiplatform module with no Android
// imports. Today it has one target (the JVM, which Android consumes). Adding
// the web build later is a matter of declaring a second target here, e.g.
// `wasmJs { browser() }`, with no code moving.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
