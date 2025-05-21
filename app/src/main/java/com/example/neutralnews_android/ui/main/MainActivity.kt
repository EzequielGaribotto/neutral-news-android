package com.example.neutralnews_android.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.neutralnews_android.R
import com.example.neutralnews_android.data.Constants.ApiObject.TYPE
import com.example.neutralnews_android.data.Constants.BottomMenu.OPTION_SETTINGS
import com.example.neutralnews_android.data.Constants.BottomMenu.OPTION_TODAY
import com.example.neutralnews_android.data.bean.settings.SettingsBean
import com.example.neutralnews_android.databinding.ActivityMainBinding
import com.example.neutralnews_android.di.view.AppActivity
import com.example.neutralnews_android.ui.main.settings.SettingsFragment
import com.example.neutralnews_android.ui.main.today.TodayFragment
import com.example.neutralnews_android.util.fullScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Actividad principal de la aplicación Neutral News.
 *
 * Esta actividad sirve como punto de entrada a la aplicación y contiene la navegación
 * principal entre las diferentes secciones como "Hoy", "Explorar" y "Buscar".
 *
 * @property connectionHandler Manejador de la conexión de red.
 * @property isHome Indica si el usuario se encuentra en la sección principal ("Hoy").
 * @property binding Objeto de enlace de datos para la interfaz de usuario.
 * @property vm Instancia del ViewModel asociado a esta actividad.
 */
@AndroidEntryPoint
class MainActivity : AppActivity() {

    private var isHome = true

    companion object {
        /**
         * Crea un nuevo Intent para iniciar la actividad [MainActivity].
         *
         * @param c Contexto desde donde se lanza el Intent.
         * @param type Tipo de dato extra (por defecto 0).
         * @return Un Intent para iniciar la actividad [MainActivity].
         */
        fun newIntent(c: Context, type: Int = 0): Intent {
            val intent = Intent(c, MainActivity::class.java)
            intent.putExtra(TYPE, type)
            return intent
        }
    }
    private var todayFragment: TodayFragment? = null
    private var settingsFragment: SettingsFragment? = null

    //Binding
    lateinit var binding: ActivityMainBinding
    private val vm: MainActivityVM by viewModels()

    // En MainActivity.kt
    private var pendingSettings: SettingsBean? = null

    @androidx.annotation.OptIn(UnstableApi::class)
    fun saveSettings(settings: SettingsBean) {
        Log.d("MainActivity", "Guardando configuración para aplicar después: $settings")
        pendingSettings = settings
    }

    // Método para guardar configuraciones para aplicarlas después
    @androidx.annotation.OptIn(UnstableApi::class)
    fun saveSettingsForLater(settings: SettingsBean) {
        Log.d("MainActivity", "Guardando configuración: $settings")
        pendingSettings = settings
    }
    /**
     * Método llamado al crear la actividad.
     *
     * Inicializa la interfaz de usuario, configura la navegación y los listeners.
     *
     * @param savedInstanceState Bundle que contiene el estado previo de la actividad (si existe).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Binding
        if (!::binding.isInitialized) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
            binding.vm = vm
        }
        //fullscreen
        fullScreen()

        binding.selected = OPTION_TODAY
        changeFontFamily(binding.txtToday)

        //On Click
        vm.obrClick.observe(this) { view ->
            try {
                if (view.tag != null) {
                    val id = view.tag as Int
                    binding.selected = id
                    when (id) {
                        OPTION_TODAY -> {
                            if (isHome && todayFragment != null) {
                                todayFragment?.scrollToTop()
                            } else {
                                changeFragment(R.id.todayFragment)
                            }
                        }
                        OPTION_SETTINGS -> {
                            changeFragment(R.id.settingsFragment)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Método llamado al presionar el botón de "Atrás".
     *
     * Determina si se debe cerrar la aplicación o navegar hacia atrás dentro de la misma.
     *
     */
    @SuppressLint("MissingSuperCall")
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isHome) {
            super.onBackPressed()
        } else {
            binding.selected = OPTION_TODAY
            changeFragment(R.id.todayFragment)
        }
    }

    /**
     * Método llamado al iniciar la actividad.
     *
     * Obtiene los datos del usuario y los asigna al objeto de enlace de datos.
     */
    override fun onStart() {
        super.onStart()
        try {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cambia el fragmento actual en la navegación.
     *
     * @param fragment ID del fragmento al que se debe navegar.
     */
    private fun changeFragment(fragmentId: Int) {
        isHome = fragmentId == R.id.todayFragment

        val transaction = supportFragmentManager.beginTransaction()


        when (fragmentId) {
            R.id.todayFragment -> {
                transaction.setCustomAnimations(
                    R.anim.slide_in_left_fast,
                    R.anim.slide_out_right_fast
                )
                if (todayFragment == null) {
                    todayFragment = TodayFragment()
                    transaction.replace(R.id.navMain, todayFragment!!, "TodayFragment")
                } else {
                    transaction.replace(R.id.navMain, todayFragment!!)
                }
                changeFontFamily(binding.txtToday)
            }

            R.id.settingsFragment -> {
                transaction.setCustomAnimations(
                    R.anim.slide_in_left_fast,
                    R.anim.slide_out_right_fast
                )
                if (settingsFragment == null) {
                    settingsFragment = SettingsFragment()
                    transaction.replace(R.id.navMain, settingsFragment!!, "SettingsFragment")
                } else {
                    transaction.replace(R.id.navMain, settingsFragment!!)
                }
                changeFontFamily(binding.txtSettings)
            }
        }

        // Compromete la transacción inmediatamente sin permitir cambios de estado
        transaction.commitNow()
    }

    /**
     * Cambia la familia tipográfica de los TextViews en el menú inferior.
     *
     * Resalta el TextView seleccionado cambiando su tipo de letra y color.
     *
     * @param textView TextView que se debe resaltar.
     */
    private fun changeFontFamily(textView: TextView?) {
        val typefaceRegular = ResourcesCompat.getFont(this, R.font.montserrat_regular)
        val typefaceMedium = ResourcesCompat.getFont(this, R.font.montserrat_medium)

        binding.txtToday.typeface = typefaceRegular
        binding.txtToday.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black50))

        binding.txtSettings.typeface = typefaceRegular
        binding.txtSettings.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black50))

        textView?.typeface = typefaceMedium
        textView?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange))
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun navigateToTodayFragment() {
        val transaction = supportFragmentManager.beginTransaction()

        // Encuentra o crea TodayFragment
        var todayFragment = supportFragmentManager.findFragmentByTag("TodayFragment") as? TodayFragment
        if (todayFragment == null) {
            todayFragment = TodayFragment()
        }

        // Reemplaza el fragmento actual con TodayFragment
        transaction.replace(R.id.navMain, todayFragment, "TodayFragment")
        // Usar commitNow() en lugar de commit() para asegurar ejecución inmediata
        transaction.commitNow()

        // Actualizar el botón seleccionado
        binding.selected = OPTION_TODAY
        changeFontFamily(binding.txtToday)

        // Aplica configuraciones pendientes si existen
        pendingSettings?.let { settings ->
            android.util.Log.d("MainActivity", "Aplicando configuración pendiente: $settings")
            // Esperar a que el fragmento esté completamente creado
            todayFragment.view?.post {
                try {
                    todayFragment.applySettings(settings)
                    android.util.Log.d("MainActivity", "Configuración aplicada con éxito")
                    pendingSettings = null
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error al aplicar configuración: ${e.message}")
                }
            }
        }
    }
}
