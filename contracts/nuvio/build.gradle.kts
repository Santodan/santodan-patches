import java.util.Properties

plugins { `java-library` }

val generated = layout.buildDirectory.dir("generated/sources/contracts")
val generateNativeContracts = tasks.register("generateNativeContracts") {
    val contracts = layout.projectDirectory.file("native.properties")
    inputs.file(contracts)
    outputs.dir(generated)
    doLast {
        val keys = contracts.asFile.readLines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("!") }
            .map { it.substringBefore('=').trim() }
        check(keys.size == keys.toSet().size) { "Duplicate key in native.properties" }
        val properties = Properties().apply { contracts.asFile.inputStream().use { load(it) } }
        val values = sortedMapOf<String, String>()
        properties.stringPropertyNames().sorted().forEach { key ->
            val value = properties.getProperty(key)
            if (key.startsWith("class.")) {
                val name = key.removePrefix("class.")
                values[name + "_NAME"] = value
                values[name + "_TYPE"] = "L" + value.replace('.', '/') + ";"
            } else values[key] = value
        }
        fun expand(key: String, visiting: Set<String> = emptySet()): String {
            check(key !in visiting) { "Circular native contract: $key" }
            val value = values[key] ?: error("Unknown native contract: $key")
            return Regex("\\$\\{([A-Z0-9_]+)}").replace(value) {
                expand(it.groupValues[1], visiting + key)
            }
        }
        val source = buildString {
            appendLine("package software.santodan.nuvio;")
            appendLine("/** Generated from contracts/nuvio/native.properties. Do not edit. */")
            appendLine("public final class NuvioNative {")
            appendLine("    private NuvioNative() {}")
            values.keys.forEach { key ->
                check(key.matches(Regex("[A-Z][A-Z0-9_]*"))) { "Invalid native contract key: $key" }
                val literal = expand(key).replace("\\", "\\\\").replace("\"", "\\\"")
                appendLine("    public static final String $key = \"$literal\";")
            }
            appendLine("}")
        }
        generated.get().file("software/santodan/nuvio/NuvioNative.java").asFile.apply {
            parentFile.mkdirs()
            writeText(source)
        }
    }
}
sourceSets.main { java.srcDir(generated) }
tasks.compileJava { dependsOn(generateNativeContracts); options.release.set(11) }
