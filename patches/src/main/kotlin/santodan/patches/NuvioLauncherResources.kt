package santodan.patches

import app.morphe.patcher.patch.resourcePatch

val nuvioLauncherArtworkResourcesPatch = resourcePatch(default = false) {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            if (!NuvioLayout.recent(manifest.getAttribute("android:versionName"))) return@use
            val application = document.getElementsByTagName("application").item(0)
            val name = "software.santodan.extension.nuviolauncher.LauncherArtworkProvider"
            val providers = document.getElementsByTagName("provider")
            check((0 until providers.length).none { providers.item(it).attributes.getNamedItem("android:name")?.nodeValue == name })
            val provider = document.createElement("provider")
            val namespace = "http://schemas.android.com/apk/res/android"
            provider.setAttributeNS(namespace, "android:name", name)
            provider.setAttributeNS(namespace, "android:authorities", manifest.getAttribute("package") + ".santodan.launcher.artwork")
            provider.setAttributeNS(namespace, "android:exported", "true")
            application.appendChild(provider)
        }
    }
}
