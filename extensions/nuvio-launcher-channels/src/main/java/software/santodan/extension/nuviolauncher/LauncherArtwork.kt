package software.santodan.extension.nuviolauncher

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import software.santodan.extension.nuviolauncher.LauncherModels.Tile

/** Render badges on a worker; expose only completed images through the artwork-only provider. */
object LauncherArtwork {
    private val workers = Executors.newFixedThreadPool(2)
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val failures = ConcurrentHashMap<String, Long>()
    private data class Style(val position: Int, val size: Int, val font: Int, val background: Int, val foreground: Int)

    private fun style(context: Context): Style {
        val prefs = context.getSharedPreferences("santodan_nuvio_date_label_style", 0)
        val defaults = intArrayOf(7, 2, 0, 6, 1)
        val colors = intArrayOf(0xff1976d2.toInt(), -1, 0xff000000.toInt(), 0xffd32f2f.toInt(),
            0xff388e3c.toInt(), 0xfff57c00.toInt(), 0xff7b1fa2.toInt(), 0xffffeb3b.toInt(),
            0xff00bcd4.toInt(), 0xffe91e63.toInt(), 0xff616161.toInt(), 0)
        fun choice(index: Int, key: String, max: Int) = prefs.getInt("date_label_$key", defaults[index]).takeIf { it in 0..max } ?: defaults[index]
        fun color(index: Int, key: String) = (prefs.getLong("date_label_${key}_color",
            colors[choice(index, key, if (index == 2) 11 else 10)].toLong() shl 32) ushr 32).toInt()
        return Style(choice(0, "position", 8), prefs.getInt("date_label_size_percent", 100).coerceIn(50, 200),
            prefs.getInt("date_label_font_size_percent", 100).coerceIn(50, 200), color(2, "background"), color(4, "foreground"))
    }

    fun uri(context: Context, tile: Tile, changed: () -> Unit): String? {
        val remote = tile.background?.takeIf { it.isNotBlank() } ?: tile.poster?.takeIf { it.isNotBlank() }
        val label = tile.dateLabel?.takeIf { it.isNotBlank() } ?: return remote
        if (remote == null) return null
        val style = style(context)
        val key = LauncherModels.key("art", remote, label, style.toString()).substringAfter(':')
        val folder = File(context.cacheDir, "santodan-launcher-artwork")
        val file = File(folder, "$key.png")
        if (file.isFile) {
            val uri = Uri.parse("content://${context.packageName}.santodan.launcher.artwork/$key.png")
            file.setLastModified(System.currentTimeMillis())
            return uri.toString()
        }
        if (System.currentTimeMillis() - (failures[key] ?: 0L) < 5 * 60_000L || !pending.add(key)) return remote
        workers.execute {
            try {
                require(folder.isDirectory || folder.mkdirs())
                render(remote, label, style, file)
                failures.remove(key)
                changed()
                val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
                folder.listFiles()?.filter { it.name.endsWith(".png") && it.lastModified() < cutoff }?.forEach { it.delete() }
            } catch (error: Exception) {
                failures[key] = System.currentTimeMillis()
                Log.w("SantodanLauncher", "Date artwork unavailable; keeping remote artwork and date text")
            } finally { pending.remove(key) }
        }
        return remote
    }

    private fun render(remote: String, label: String, style: Style, target: File) {
        val address = URL(remote)
        require(address.protocol == "https" || address.protocol == "http")
        val connection = address.openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        val bytes = try {
            require(connection.responseCode in 200..299)
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 8 * 1024 * 1024)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 1280) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Invalid artwork")
        val bitmap = try { original.copy(Bitmap.Config.ARGB_8888, true) ?: error("Artwork copy failed") } finally { original.recycle() }
        val temporary = File(target.parentFile, target.name + ".tmp")
        try {
            val canvas = Canvas(bitmap)
            val scale = bitmap.width / 300f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.foreground
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 14f * scale * style.font / 100f
            }
            val paddingX = 6f * scale * style.size / 100f
            val paddingY = 3f * scale * style.size / 100f
            val margin = 6f * scale
            val maxWidth = bitmap.width - 2 * margin
            val measured = paint.measureText(label)
            if (measured + 2 * paddingX > maxWidth) paint.textSize *= (maxWidth - 2 * paddingX).coerceAtLeast(1f) / measured
            val metrics = paint.fontMetrics
            val width = paint.measureText(label) + 2 * paddingX
            val height = metrics.descent - metrics.ascent + 2 * paddingY
            val x = when (style.position % 3) { 0 -> margin; 1 -> (bitmap.width - width) / 2; else -> bitmap.width - margin - width }
            val y = when (style.position / 3) { 0 -> margin; 1 -> (bitmap.height - height) / 2; else -> bitmap.height - margin - height }
            canvas.drawRoundRect(RectF(x, y, x + width, y + height), 4f * scale, 4f * scale,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.background })
            canvas.drawText(label, x + paddingX, y + paddingY - metrics.ascent, paint)
            temporary.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            require(temporary.renameTo(target))
        } finally { bitmap.recycle(); temporary.delete() }
    }
}
