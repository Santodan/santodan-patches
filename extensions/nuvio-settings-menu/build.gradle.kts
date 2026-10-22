extension { name = "extensions/nuvio-settings-menu.mpe" }

android {
    namespace = "software.santodan.extension.nuviomenu"
    compileOptions { isCoreLibraryDesugaringEnabled = false }
    sourceSets.getByName("main").java.srcDir("../../contracts/nuvio/src/main/java")
}

dependencies { compileOnly(project(":nuvio-contracts")) }
