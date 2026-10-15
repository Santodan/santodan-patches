import java.util.Properties

group = "software.santodan.patches"

patches {
    about {
        name = "Santodan Patches"
        description = "Independent Morphe patches for Car Scanner, MEO, NuvioTV, Reddit, Pillo, and Peafowl"
        source = "https://github.com/Santodan/santodan-patches"
        author = "Santodan"
        contact = "https://github.com/Santodan"
        website = "https://morphe.software/add-source?github=Santodan/santodan-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

// Match Android's SDK lookup on developer machines and GitHub-hosted runners.
// JVM verification sources reference Android types but must not bundle SDK stubs.
val androidSdkDirectory = providers.provider {
    val sdkProperties = Properties()
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
        sdkProperties.load(it)
    }
    sdkProperties.getProperty("sdk.dir")
        ?: providers.environmentVariable("ANDROID_HOME")
            .orElse(providers.environmentVariable("ANDROID_SDK_ROOT")).orNull
        ?: throw GradleException("Android SDK not found: set sdk.dir in local.properties or ANDROID_HOME / ANDROID_SDK_ROOT")
}
val androidTestJar = androidSdkDirectory.map { file("$it/platforms/android-36/android.jar") }

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("org.json:json:20240303")
    testCompileOnly(files(androidTestJar))
}

kotlin.sourceSets.named("test") {
    kotlin.srcDir("../extensions/nuvio-stream-preload/src/main/java")
}

sourceSets["test"].java.srcDir("../extensions/pillo-weight-import/src/main/java")
sourceSets["test"].java.srcDir("../extensions/pillo-local-backup/src/main/java")
sourceSets["test"].java.srcDir("../extensions/pillo-weight-summary/src/main/java")

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    source(file("../extensions/nuvio-launcher-channels/src/main/java/software/santodan/extension/nuviolauncher/LauncherModels.kt"))
    source(file("../extensions/nuvio-launcher-channels/src/main/java/software/santodan/extension/nuviolauncher/LauncherDates.kt"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioProviderRefresh.kt"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioSourceContinuation.java"))
}

// The Morphe patch runtime targets Java 11. Pin Java sources explicitly so
// local builds remain reproducible even when Gradle runs on a newer JDK.
tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.release.set(11)
}

// The Java files under src/test are command-line DEX verification programs,
// not JUnit tests. Compile them, but do not fail because no test engine finds
// annotated test methods.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    failOnNoDiscoveredTests.set(false)
}

// Exercise the exact Android runtime accessor table against original app DEX files.
tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileTestJava") {
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioMergePolicy.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioSourceContinuation.java"))
    source(file("../extensions/nuvio-remaining-episodes/src/main/java/software/santodan/extension/nuvioremaining/NuvioBadgeComposition.java"))
    source(file("../extensions/nuvio-airing-series/src/main/java/software/santodan/extension/nuvioairing/NuvioBadgeComposition.java"))
    source(file("../extensions/nuvio-movie-release-dates/src/main/java/software/santodan/extension/nuviomovierelease/MovieReleaseDate.java"))
    source(file("../extensions/nuvio-movie-release-dates/src/main/java/software/santodan/extension/nuviomovierelease/NuvioBadgeComposition.java"))
    source(file("../extensions/nuvio-finale-dates/src/main/java/software/santodan/extension/nuviofinale/NuvioBadgeComposition.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioProviderLayout.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioWatchedHistory.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioBadgeDelta.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioProviderBadge.java"))
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioSettingsStoreResolver.java"))
    source(file("../extensions/nuvio-remaining-episodes/src/main/java/software/santodan/extension/nuvioremaining/NuvioEpisodeCounts.java"))
}

tasks {
    register<JavaExec>("verifyCarScannerManaged") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyCarScannerManaged")
        maxHeapSize = "1g"
        args(file("../../OriginalApps/Car-Scanner-ELM-OBD2_2.1.50.apk").absolutePath)
    }
    register<JavaExec>("verifyCarScannerManagedBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyCarScannerManagedBundleKt")
        maxHeapSize = "2g"
        args(file("../../OriginalApps/Car-Scanner-ELM-OBD2_2.1.50.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/carscanner-managed-bundle").absolutePath)
    }
    register<JavaExec>("verifyNuvioLauncherUi") {
        dependsOn("testClasses", ":extensions:nuvio-launcher-channels:compileReleaseKotlin", ":extensions:nuvio-launcher-channels:compileReleaseJavaWithJavac")
        classpath = sourceSets["test"].runtimeClasspath + files(
            project(":extensions:nuvio-launcher-channels").layout.buildDirectory.dir("intermediates/built_in_kotlinc/release/compileReleaseKotlin/classes"),
            project(":extensions:nuvio-launcher-channels").layout.buildDirectory.dir("intermediates/javac/release/compileReleaseJavaWithJavac/classes"))
        mainClass.set("santodan.patches.VerifyNuvioLauncherUi")
        args(file("${layout.buildDirectory.get()}/verification/launcher-ui").absolutePath)
    }
    register<JavaExec>("verifyNuvioLauncherDates") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioLauncherDatesKt")
    }
    register<JavaExec>("verifyNuvioLauncherPublisher") {
        dependsOn("testClasses", ":extensions:nuvio-launcher-channels:compileReleaseKotlin", ":extensions:nuvio-launcher-channels:compileReleaseJavaWithJavac")
        classpath = sourceSets["test"].runtimeClasspath + files(
            project(":extensions:nuvio-launcher-channels").layout.buildDirectory.dir("intermediates/built_in_kotlinc/release/compileReleaseKotlin/classes"),
            project(":extensions:nuvio-launcher-channels").layout.buildDirectory.dir("intermediates/javac/release/compileReleaseJavaWithJavac/classes"))
        mainClass.set("santodan.patches.VerifyNuvioLauncherPublisher")
        args(file("${layout.buildDirectory.get()}/verification/launcher-publisher").absolutePath)
    }
    register<JavaExec>("verifyNuvioLauncherBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioLauncherBundleKt")
        maxHeapSize = "4g"
        args(file("../../OriginalApps/NuvioTV-1.1.0-beta6.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/nuvio-launcher-bundle").absolutePath)
    }
    register<JavaExec>("verifyNuvioLauncherChannels") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioBeta6LauncherKt")
        args(file("../../.inspect-nuvio-beta6").absolutePath)
    }
    register<JavaExec>("verifyNuvioDateLabelStyleRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioBeta6DateStyleRuntime")
        doFirst { file("${layout.buildDirectory.get()}/verification").mkdirs() }
        args(file("../extensions/nuvio-settings-menu/src/main/java/software/santodan/extension/nuviomenu").absolutePath,
            file("${layout.buildDirectory.get()}/verification").absolutePath)
    }
    register<JavaExec>("verifyNuvioRemainingSyncRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioRemainingSyncRuntime")
        doFirst { file("${layout.buildDirectory.get()}/verification").mkdirs() }
        args(file("../extensions/nuvio-remaining-episodes/src/main/java/software/santodan/extension/nuvioremaining").absolutePath,
            file("${layout.buildDirectory.get()}/verification").absolutePath)
    }
    register<JavaExec>("verifyNuvioProviderRefreshRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioProviderRefreshRuntimeKt")
    }
    register<JavaExec>("verifyPilloWeightSummaryBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloWeightSummaryBundleKt")
        maxHeapSize = "4g"
        args(file("../../.inspect-pillo-620/xyz.rtrvr.pillo.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/pillo-summary-bundle").absolutePath,
            file("../../Apps/Pillo/Pillo-0.6.20-patches-1.46.0.apk").absolutePath)
    }
    register<JavaExec>("verifyPilloLocalArchive") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloLocalArchive")
        args(file("${layout.buildDirectory.get()}/verification").absolutePath)
    }
    register<JavaExec>("verifyPilloLocalBackup") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloLocalBackup")
        args(file("../../.inspect-pillo-620").absolutePath,
            file("${layout.buildDirectory.get()}/verification/pillo-local-backup.dex").absolutePath)
    }
    register<JavaExec>("verifyPilloLocalBackupBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloLocalBackupBundleKt")
        maxHeapSize = "4g"
        args(file("../../.inspect-pillo-620/xyz.rtrvr.pillo.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/pillo-local-bundle").absolutePath)
    }

    register<JavaExec>("verifyPilloWeightImportBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloWeightImportBundleKt")
        maxHeapSize = "4g"
        args(file("../../.inspect-pillo-620/xyz.rtrvr.pillo.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/pillo-import-bundle").absolutePath)
    }

    register<JavaExec>("verifyPilloWeightImport") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloWeightImport")
        args(file("../../.inspect-pillo-620").absolutePath,
            file("${layout.buildDirectory.get()}/verification/pillo-weight-import.dex").absolutePath)
    }

    register<JavaExec>("verifyPilloWeightImportRuntime") {
        dependsOn("testClasses")
        // Android's org.json classes are JVM stubs; use the real JSON library first.
        classpath = sourceSets["test"].runtimeClasspath.filter { it.name != "android.jar" } +
            files(androidTestJar)
        mainClass.set("santodan.patches.VerifyPilloWeightImportRuntimeKt")
        providers.gradleProperty("weightBackup").orNull?.let { args(it) }
    }
    register<JavaExec>("verifyNuvioProviderBadge") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioProviderBadge")
    }
    register<JavaExec>("verifyNuvioMovieReleaseRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioMovieReleaseRuntime")
        args(file("../extensions/nuvio-movie-release-dates/src/main/java/software/santodan/extension/nuviomovierelease").absolutePath, file("${layout.buildDirectory.get()}/verification/movie-release-runtime").absolutePath)
    }
    register<JavaExec>("verifyNuvioMovieReleaseDates") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioMovieReleaseDates")
    }
    register<JavaExec>("verifyNuvioStreamPreload") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioStreamPreloadKt")
    }
    register<JavaExec>("verifyNuvioStreamPreloadRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioStreamPreloadRuntimeKt")
    }
    register<JavaExec>("verifyNuvioNextEpisodePreload") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioNextEpisodePreloadKt")
    }
    register<JavaExec>("verifyNuvioNextEpisodeBundle") {
        dependsOn("testClasses", "buildAndroid")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioBeta6BundleKt")
        maxHeapSize = "4g"
        args(file("../../OriginalApps/NuvioTV-1.1.0-beta6.apk").absolutePath,
            file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
            file("${layout.buildDirectory.get()}/verification/nuvio-next-episode-bundle").absolutePath)
    }
    register<JavaExec>("verifyNuvioBadgeComposition") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioBadgeComposition")
    }

    register<JavaExec>("verifyNuvioBadgeDelta") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioBadgeDelta")
    }

    register<JavaExec>("verifyNuvioWatchedHistory") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioWatchedHistory")
    }

    register<JavaExec>("verifyNuvioRemainingCounts") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioRemainingCounts")
    }

    register<JavaExec>("verifyNuvioSettingsStoreRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioSettingsStoreRuntime")
    }

    register<JavaExec>("verifyNuvioSettingsMenuRuntime") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyNuvioSettingsMenuRuntime")
        args(file("../extensions/nuvio-settings-menu/src/main/java/software/santodan/extension/nuviomenu/NuvioSettingsMenu.java").absolutePath,
            file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioSourceContinuation.java").absolutePath,
            file("${layout.buildDirectory.get()}/verification/settings-menu-runtime").absolutePath)
    }




    register<JavaExec>("verifyRedditContentFilter") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyRedditContentFilter")
        args(fileTree("../../.inspect-reddit") { include("classes*.dex") }.files.sorted().map { it.absolutePath })
    }

    register<JavaExec>("verifyRedditGuestMode") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyRedditGuestMode")
        args(fileTree("../../.inspect-reddit") { include("classes*.dex") }.files.sorted().map { it.absolutePath })
    }

    register<JavaExec>("verifyRedditPostBatch") {
        dependsOn("testClasses", ":extensions:reddit-filter:compileReleaseJavaWithJavac")
        classpath = sourceSets["test"].runtimeClasspath + files(androidTestJar,
            project(":extensions:reddit-filter").layout.buildDirectory.dir(
                "intermediates/javac/release/compileReleaseJavaWithJavac/classes"))
        mainClass.set("santodan.patches.VerifyRedditPostBatch")
    }

    register<JavaExec>("verifyPilloPatch") {
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("santodan.patches.VerifyPilloPatch")
        val input = file("../../.inspect-pillo-620/classes15.dex")
        args(input.absolutePath, file("${layout.buildDirectory.get()}/verification/pillo-classes15.dex").absolutePath)
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

tasks.register<JavaExec>("verifyNuvioBeta6") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("santodan.patches.VerifyNuvioBeta6")
    maxHeapSize = "2g"
    args("1.1.0-beta.6", file("../../.inspect-nuvio-beta6").absolutePath,
        file("${layout.buildDirectory.get()}/verification/nuvio-beta6.dex").absolutePath)
}

sourceSets["test"].java.srcDir("../extensions/nuvio-stream-preload/src/main/java")

tasks.register<JavaExec>("verifyNuvioBeta6Bundle") {
    dependsOn("testClasses", "buildAndroid")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("santodan.patches.VerifyNuvioBeta6BundleKt")
    maxHeapSize = "3g"
    args(file("../../OriginalApps/NuvioTV-1.1.0-beta6.apk").absolutePath,
        file("${layout.buildDirectory.get()}/libs/patches-${project.version}.mpp").absolutePath,
        file("${layout.buildDirectory.get()}/verification/nuvio-beta6-bundle").absolutePath,
        file(androidSdkDirectory.get()).absolutePath)
}

tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileTestJava") {
    source(file("../extensions/nuvio-merged-progress/src/main/java/software/santodan/extension/nuviomerged/NuvioBeta6.java"))
}

tasks.register<JavaExec>("verifyNuvioBeta6DateStyleRuntime") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("santodan.patches.VerifyNuvioBeta6DateStyleRuntime")
    args(file("../extensions/nuvio-settings-menu/src/main/java/software/santodan/extension/nuviomenu").absolutePath,
        file("${layout.buildDirectory.get()}/verification").absolutePath)
}
tasks.register<JavaExec>("verifyNuvioBeta6Launcher") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("santodan.patches.VerifyNuvioBeta6LauncherKt")
    args(file("../../.inspect-nuvio-beta6").absolutePath)
}

tasks.register<JavaExec>("verifyNuvioBeta6Player") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("santodan.patches.VerifyNuvioBeta6PlayerKt")
    args(file("${layout.buildDirectory.get()}/verification").absolutePath)
}

tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileTestJava") {
    source(file("../extensions/nuvio-settings-menu/src/main/java/software/santodan/extension/nuviomenu/NuvioBeta6.java"))
}
