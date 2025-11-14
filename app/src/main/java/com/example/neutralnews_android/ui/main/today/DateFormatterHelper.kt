package com.example.neutralnews_android.ui.main.today

import android.util.Log
import com.example.neutralnews_android.data.Constants.DateFormat.DATE_FORMAT
import com.google.firebase.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper class for date formatting and parsing operations.
 */
object DateFormatterHelper {
    private val dateFormatCache = ConcurrentHashMap<String, String?>()

    /**
     * Formatea una fecha en formato español.
     */
    fun formatDateToSpanish(value: Any?): String? {
        if (value == null) return null

        // Si es String y está en caché, devolver directamente
        if (value is String && dateFormatCache.containsKey(value)) {
            return dateFormatCache[value]
        }

        val outputFormat = SimpleDateFormat(DATE_FORMAT, Locale("es", "ES"))
        val date: Date? = when (value) {
            is Timestamp -> value.toDate()
            is Date -> value
            is String -> parseStringDate(value)
            is Long -> Date(value)
            else -> null
        }

        val result = date?.let { outputFormat.format(it).uppercase() }

        // Guardar en caché si es una cadena
        if (value is String && result != null) {
            dateFormatCache[value] = result
        }

        return result
    }

    /**
     * Helper function to parse date strings in various formats
     */
    private fun parseStringDate(dateString: String): Date? {
        val inputFormats = listOf(
            SimpleDateFormat(DATE_FORMAT, Locale("es", "ES")),
            SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")),
        )

        for (format in inputFormats) {
            try {
                val parsedDate = format.parse(dateString)
                if (parsedDate != null) return parsedDate
            } catch (_: ParseException) {
                // Intentar con el siguiente formato
            }
        }

        Log.e("DateDebug", "No se pudo parsear la fecha: '$dateString'")
        return null
    }

    /**
     * Convierte una cadena de fecha a un objeto Date para comparaciones.
     */
    fun getDateForSorting(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val formats = listOf(DATE_FORMAT)

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale("es", "ES"))
                val date = sdf.parse(dateString)

                if (format == DATE_FORMAT) {
                    val calendar = Calendar.getInstance()
                    if (date != null) calendar.time = date
                    calendar.set(Calendar.YEAR, currentYear)
                    return calendar.time
                }

                return date
            } catch (_: Exception) {
                continue
            }
        }

        return null
    }

    /**
     * Parsea un string de fecha y devuelve el timestamp en millis.
     */
    fun parseDateToTimestamp(dateString: String?): Long {
        return getDateForSorting(dateString)?.time ?: 0L
    }

    /**
     * Calcula las fechas de los últimos 7 días.
     */
    fun calculatePastDays(): Map<String, Date> {
        val pastDays = mutableMapOf<String, Date>()
        val calendar = Calendar.getInstance()

        pastDays["Hoy"] = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        pastDays["Ayer"] = calendar.time

        val dayFormat = SimpleDateFormat("EEEE", Locale("es", "ES"))
        for (i in 2..6) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val dayName = dayFormat.format(calendar.time).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            pastDays[dayName] = calendar.time
        }

        return pastDays
    }
}

