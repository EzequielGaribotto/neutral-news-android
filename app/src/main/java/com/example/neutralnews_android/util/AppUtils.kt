@file:Suppress("unused")

package com.example.neutralnews_android.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Patterns
import com.example.neutralnews_android.R
import java.net.URL
import java.util.Calendar
import kotlin.math.roundToInt

object AppUtils {

    fun getVersionCode(context: Context): String {
        return String.format(context.resources.getString(R.string.version_count), context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt().toString())
    }

    fun getVersionName(context: Context): String {
        return String.format(context.resources.getString(R.string.version_count), context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString())
    }

    //Get name with extension from URL
    fun getFileNameFromURL(url: String?): String {
        if (url == null) {
            return ""
        }
        try {
            val resource = URL(url)
            val host = resource.host
            if (host.isNotEmpty() && url.endsWith(host)) {
                return ""
            }
        } catch (e: Exception) {
            return ""
        }
        val startIndex = url.lastIndexOf('/') + 1
        val length = url.length

        // find end index for ?
        var lastQMPos = url.lastIndexOf('?')
        if (lastQMPos == -1) {
            lastQMPos = length
        }

        // find end index for #
        var lastHashPos = url.lastIndexOf('#')
        if (lastHashPos == -1) {
            lastHashPos = length
        }

        // calculate the end index
        val endIndex = lastQMPos.coerceAtMost(lastHashPos)
        return url.substring(startIndex, endIndex)
    }

    //Monthly
    fun getMonthStartEndTimestamps(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Set the calendar to the specified year and month
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1) // Month in Calendar class is 0-based

        // Set the calendar to the first day of the specified month
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        //Start Time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTimeInMillis = calendar.timeInMillis

        // Set the calendar to the last day of the specified month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

        //End Time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)

        val endTimeInMillis = calendar.timeInMillis

        return Pair(startTimeInMillis, endTimeInMillis)
    }

    fun getCurrentMonthAndYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Adding 1 since months are 0-based
        return Pair(currentYear, currentMonth)
    }

    fun getNextMonthAndYear(currentYear: Int, currentMonth: Int): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth - 1) // Month in Calendar class is 0-based
        calendar.add(Calendar.MONTH, 1) // Add one month to the current month
        val nextYear = calendar.get(Calendar.YEAR)
        val nextMonth = calendar.get(Calendar.MONTH) + 1 // Adding 1 since months are 0-based
        return Pair(nextYear, nextMonth)
    }

    fun getPreviousMonthAndYear(currentYear: Int, currentMonth: Int): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth - 1) // Month in Calendar class is 0-based
        calendar.add(Calendar.MONTH, -1) // Subtract one month from the current month
        val previousYear = calendar.get(Calendar.YEAR)
        val previousMonth = calendar.get(Calendar.MONTH) + 1 // Adding 1 since months are 0-based
        return Pair(previousYear, previousMonth)
    }

    //Weekly
    fun getCurrentWeekStartEndTimestamps(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // Set to the first day of the week (usually Sunday or Monday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfWeekInMillis = calendar.timeInMillis

        // Move calendar to the end of the week (usually Saturday or Sunday)
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        //End Time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)

        val endOfWeekInMillis = calendar.timeInMillis

        return Pair(startOfWeekInMillis, endOfWeekInMillis)
    }

    fun getNextWeekStartEndTimestamps(@Suppress("UNUSED_PARAMETER") startOfWeekMillis: Long, endOfWeekMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Set the calendar to the end of the current week
        calendar.timeInMillis = endOfWeekMillis

        // Move to the next week
        calendar.add(Calendar.SECOND, 1)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // Set to the first day of the week (usually Sunday or Monday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val nextWeekStart = calendar.timeInMillis

        // Move calendar to the end of the next week (usually Saturday or Sunday)
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        //End Time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)

        val nextWeekEnd = calendar.timeInMillis

        return Pair(nextWeekStart, nextWeekEnd)
    }

    fun getPreviousWeekStartEndTimestamps(startOfWeekMillis: Long, @Suppress("UNUSED_PARAMETER") endOfWeekMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Set the calendar to the start of the current week
        calendar.timeInMillis = startOfWeekMillis

        // Move to the previous week
        calendar.add(Calendar.MILLISECOND, -1)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // Set to the first day of the week (usually Sunday or Monday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val previousWeekStart = calendar.timeInMillis

        // Move calendar to the end of the previous week (usually Saturday or Sunday)
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        //End Time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)

        val previousWeekEnd = calendar.timeInMillis

        return Pair(previousWeekStart, previousWeekEnd)
    }

    //Day
    fun getNextDayTimestamp(currentDayMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDayMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Add one day to the current day

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimeInMillis = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)
        val endTimeInMillis = calendar.timeInMillis

        return Pair(startTimeInMillis, endTimeInMillis)
    }

    fun getPreviousDayTimestamp(currentDayMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDayMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Subtract one day from the current day

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimeInMillis = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)
        val endTimeInMillis = calendar.timeInMillis

        return Pair(startTimeInMillis, endTimeInMillis)
    }

    fun getCurrentDayTimestamp(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimeInMillis = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)
        val endTimeInMillis = calendar.timeInMillis

        return Pair(startTimeInMillis, endTimeInMillis)
    }


    fun getStartTImeStamp(startOfWeekMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startOfWeekMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndTImeStamp(endOfWeekMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endOfWeekMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }


    //Get android id
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            ""
        }
    }

    //Get device id
    @SuppressLint("HardwareIds")
    fun getDeviceId(c: Context): String {
        return Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID)
    }

    //Convert dp to pixel
    fun convertDpToPixel(dp: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        val px = dp * (metrics.densityDpi / 160f)
        return px.roundToInt()
    }

    //Get address from latitude and longitude
    fun getAddress(latitude: String, longitude: String, context: Context): String {
        val geocoder = Geocoder(context)
        @Suppress("DEPRECATION") val addressList = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)
        if (addressList?.size!! > 0) {
            val address = StringBuffer()
            val addressData = addressList[0]
            if (addressData.locality != null) {
                address.append(addressData.locality).append(", ")
            }
            address.append(addressData.adminArea)

            return address.toString()
        }
        return ""
    }

    //Check internet connection
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return true
                }
            }
        }
        return false
    }

    fun getTimeStringFromMs(time: Long, needMs: Boolean = false): String {
        val hours = (time / (1000 * 60 * 60)).toInt()
        val minutes = ((time % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val seconds = ((time % (1000 * 60)) / 1000).toInt()
        return if (needMs) {
            val ms = time % 1000
            String.format("%02d:%02d.%03d", minutes, seconds, ms)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    //Check email is valid or not
    fun emailInvalid(email: String?): Boolean {
        return if (email == null) true else !Patterns.EMAIL_ADDRESS.matcher(email.trim { it <= ' ' }).matches()
    }
}