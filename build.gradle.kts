// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    dependencies {
        // Classpath dependencies for the Android and Firebase plugins
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath("com.google.gms:google-services:4.4.2")
    }
}

task("clean") {
    delete(rootProject.buildDir)
}