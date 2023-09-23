pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // you can add mirror repositories by:
        // maven { url = uri("THE URL") }
    }
}

val projName: String by settings
rootProject.name = projName
