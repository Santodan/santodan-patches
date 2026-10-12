package software.santodan.extension.nuviolauncher

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LauncherDates {
    fun label(released: String?, fallback: String?, locale: Locale = Locale.getDefault()): String? {
        val date = released?.let { value ->
            runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
                ?: runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
        }
        return date?.format(DateTimeFormatter.ofPattern("dd-MMM", locale)) ?: fallback?.takeIf { it.isNotBlank() }
    }
}
