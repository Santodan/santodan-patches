extension {
    name = "extensions/nuvio-remaining-episodes.mpe"
}

android {
    namespace = "software.santodan.extension.nuvioremaining"

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies { compileOnly(project(":nuvio-contracts")) }
