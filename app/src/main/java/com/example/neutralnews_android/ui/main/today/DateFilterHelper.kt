package com.example.neutralnews_android.ui.main.today

import com.example.neutralnews_android.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for managing date filter persistence and operations.
 */
object DateFilterHelper {

    /**
     * Persiste las fechas seleccionadas en SharedPreferences.
     */
    fun persistSelectedDates(selectedDates: List<Date>) {
        if (selectedDates.isEmpty()) {
            Prefs.remove("selected_dates")
        } else {
            val millisSet = selectedDates.map { it.time.toString() }.toSet()
            Prefs.putStringSet("selected_dates", millisSet)
        }
    }

    /**
     * Restaura las fechas seleccionadas desde SharedPreferences.
     */
    fun restoreSelectedDates(): List<Date> {
        return try {
            val saved = Prefs.getStringSet("selected_dates", null)
            if (saved != null && saved.isNotEmpty()) {
                val arr = saved.mapNotNull { it?.toLongOrNull() }
                arr.map { Date(it) }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Formatea las fechas seleccionadas para mostrar en UI.
     */
    fun formatSelectedDates(selectedDates: List<Date>): Set<String> {
        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        return selectedDates.map { fmt.format(it) }.toSet()
    }

    /**
     * Convierte las fechas seleccionadas a millis array.
     */
    fun datesToMillisArray(selectedDates: List<Date>): LongArray {
        return selectedDates.map { it.time }.toLongArray()
    }

    /**
     * Limpia las fechas seleccionadas persistidas.
     */
    fun clearSelectedDates() {
        Prefs.remove("selected_dates")
    }
}

