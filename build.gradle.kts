plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.18" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")

    }
}
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
////        gradlePluginPortal()
//    }
//    dependencies {
//        classpath ("com.android.tools.build:gradle:7.4.2")
//        classpath ("com.google.gms:google-services:4.4.1")
//    }
//}
