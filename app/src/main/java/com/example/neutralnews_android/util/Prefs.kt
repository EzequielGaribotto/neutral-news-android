package com.example.neutralnews_android.util


import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.neutralnews_android.di.NeutralNewsApplication

@Suppress("unused")
object Prefs {
    private const val LENGTH = "#LENGTH"
    private val mPrefs: SharedPreferences = NeutralNewsApplication.applicationContext().getSharedPreferences(NeutralNewsApplication.applicationContext().packageName, ContextWrapper.MODE_PRIVATE)

    fun getAll(): Map<String, *> {
        return mPrefs.all
    }

    fun getInt(key: String, defValue: Int): Int {
        return mPrefs.getInt(key, defValue)
    }

    fun getInt(key: String): Int {
        return mPrefs.getInt(key, 0)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return mPrefs.getBoolean(key, defValue)
    }

    fun getBoolean(key: String): Boolean {
        return mPrefs.getBoolean(key, false)
    }

    fun getLong(key: String, defValue: Long): Long {
        return mPrefs.getLong(key, defValue)
    }

    fun getLong(key: String): Long {
        return mPrefs.getLong(key, 0L)
    }

    fun getDouble(key: String, defValue: Double): Double {
        return java.lang.Double.longBitsToDouble(mPrefs.getLong(key, java.lang.Double.doubleToLongBits(defValue)))
    }

    fun getDouble(key: String): Double {
        return java.lang.Double.longBitsToDouble(mPrefs.getLong(key, java.lang.Double.doubleToLongBits(0.0)))
    }

    fun getFloat(key: String, defValue: Float): Float {
        return mPrefs.getFloat(key, defValue)
    }

    fun getFloat(key: String): Float {
        return mPrefs.getFloat(key, 0.0f)
    }

    fun getString(key: String, defValue: String?): String? {
        return mPrefs.getString(key, defValue)
    }

    fun getString(key: String): String? {
        return mPrefs.getString(key, "")
    }

    fun getStringSet(key: String, defValue: Set<String?>?): Set<String?>? {
        return mPrefs.getStringSet(key, defValue)
    }

    fun putLong(key: String, value: Long) {
        val editor = mPrefs.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun putInt(key: String, value: Int) {
        val editor = mPrefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun putDouble(key: String, value: Double) {
        val editor = mPrefs.edit()
        editor.putLong(key, java.lang.Double.doubleToRawLongBits(value))
        editor.apply()
    }

    fun putFloat(key: String, value: Float) {
        val editor = mPrefs.edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = mPrefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun putString(key: String, value: String?) {
        val editor = mPrefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun putStringSet(key: String, value: Set<String?>) {
        val editor = mPrefs.edit()
        editor.putStringSet(key, value)
        editor.apply()
    }

    fun remove(key: String) {
        val editor = mPrefs.edit()
        if (mPrefs.contains(key + LENGTH)) {
            val stringSetLength = mPrefs.getInt(key + LENGTH, -1)
            if (stringSetLength >= 0) {
                editor.remove(key + LENGTH)
                for (i in 0 until stringSetLength) {
                    editor.remove("$key[$i]")
                }
            }
        }
        editor.remove(key)
        editor.apply()
    }

    fun contains(key: String): Boolean {
        return mPrefs.contains(key)
    }

    fun clear(): SharedPreferences.Editor {
        val editor = mPrefs.edit().clear()
        editor.apply()
        return editor
    }

    fun edit(): SharedPreferences.Editor {
        return mPrefs.edit()
    }
}