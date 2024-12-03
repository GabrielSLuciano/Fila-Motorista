// Arquivo de configuração de nível superior onde você pode adicionar opções comuns para todos os subprojetos/módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    dependencies {
        classpath(libs.google.services)
    }
}

