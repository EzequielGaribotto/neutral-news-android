package com.example.neutralnews_android.di

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.example.neutralnews_android.data.remote.helper.NetworkErrorHandler
import com.example.neutralnews_android.ui.main.MainActivity
import com.example.neutralnews_android.util.preferences.Prefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase personalizada de la aplicación que integra Dagger para inyección de dependencias.
 */
@HiltAndroidApp
class NeutralNewsApplication : Application() {
    var networkErrorHandler: NetworkErrorHandler? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Habilitar modo oscuro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Inicializar servicios
        initWebServices()
    }

    private fun initWebServices() {
        networkErrorHandler = NetworkErrorHandler(this)
        // Resto de inicialización...
    }


    fun restartApp() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Prefs.clear()
                val intent: Intent = MainActivity.newIntent(this@NeutralNewsApplication)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        lateinit var instance: NeutralNewsApplication

        fun applicationContext(): Context {
            return instance.applicationContext
        }
    }
}
