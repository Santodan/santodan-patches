extension { name = "extensions/nuvio-launcher-channels.mpe" }
android {
    namespace = "software.santodan.extension.nuviolauncher"
    compileOptions { isCoreLibraryDesugaringEnabled = false }
}
dependencies { compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") }

dependencies { compileOnly(project(":nuvio-contracts")) }
