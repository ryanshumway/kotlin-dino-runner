// Plugins are declared once here with `apply false` so every module resolves
// the same versions from gradle/libs.versions.toml, then each module applies
// only the ones it needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
