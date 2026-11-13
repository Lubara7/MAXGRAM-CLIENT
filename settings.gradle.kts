pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\.testing.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\.testing.*")
            }
        }
        mavenCentral()
    }
}
rootProject.name = "MAXGRAM-CLIENT"
include(":max-backend", ":auth-harness", ":core:domain", ":core:data", ":core:ui", ":core:localization", ":feature:auth")

project(":auth-harness").projectDir = file("tools-auth-harness")
