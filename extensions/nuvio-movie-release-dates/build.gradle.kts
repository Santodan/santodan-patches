extension {
    name = "extensions/nuvio-movie-release-dates.mpe"
}

android {
    namespace = "software.santodan.extension.nuviomovierelease"

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies { compileOnly(project(":nuvio-contracts")) }
