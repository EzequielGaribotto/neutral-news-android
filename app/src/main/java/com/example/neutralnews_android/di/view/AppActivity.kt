package com.example.neutralnews_android.di.view

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener
import com.example.neutralnews_android.BuildConfig
import com.example.neutralnews_android.R
import com.example.neutralnews_android.databinding.DialogLoaderBinding
import com.example.neutralnews_android.di.dialog.BaseCustomDialog
import com.example.neutralnews_android.util.message.MessageUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import org.aviran.cookiebar2.CookieBar


/**
 * Clase base abstracta para actividades que proporciona soporte para localización, mensajes tipo cookie, manejo
 * de diálogos de progreso y métodos para gestionar actividades y transiciones. Esta actividad sirve como clase
 * base común para todas las actividades.
 */



@AndroidEntryPoint
abstract class AppActivity : AppCompatActivity(), OnLocaleChangedListener {

    private var dialogLoader: BaseCustomDialog<DialogLoaderBinding>? = null

    private val localizationDelegate by lazy {
        LocalizationActivityDelegate(this)
    }
    /**
     * Se llama cuando la actividad es creada. Inicializa el delegado de localización y la actividad.
     *
     * @param savedInstanceState El estado guardado de la actividad.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkGooglePlayServices()
        localizationDelegate.addOnLocaleChangedListener(this)
        localizationDelegate.onCreate()
    }

    private fun checkGooglePlayServices() {
        if (BuildConfig.DEBUG) {
            // En modo debug/desarrollo, solo registrar el estado
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    Log.d("GooglePlayServices", "Google Play Services está disponible")
                    return
                }
                else -> {
                    Log.w("GooglePlayServices", "Google Play Services no está disponible: $resultCode")
                    // Continuar de todos modos en modo debug
                    return
                }
            }
        } else {
            // En producción, realizar la verificación completa
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

            when (resultCode) {
                ConnectionResult.SUCCESS -> return
                ConnectionResult.SERVICE_MISSING,
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                ConnectionResult.SERVICE_UPDATING -> {
                    val dialog = googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                    dialog?.show() ?: run {
                        msgError(getString(R.string.error_play_services))
                        finish()
                    }
                }
                else -> {
                    msgError(getString(R.string.error_play_services))
                    finish()
                }
            }
        }
    }

    companion object {
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 2404
    }

    /**
     * Muestra un mensaje normal en forma de cookie.
     *
     * @param msg El mensaje a mostrar.
     */
    fun msgNormal(msg: String) {
        MessageUtils.normal(this, msg)
    }

    /**
     * Muestra un mensaje de éxito en forma de cookie.
     *
     * @param msg El mensaje a mostrar.
     */
    fun msgSuccess(msg: String) {
        //MessageUtils.success(this, msg)
        showCookie(msg, R.color.successColor)
    }

    /**
     * Muestra un mensaje informativo en forma de cookie.
     *
     * @param msg El mensaje a mostrar.
     */
    fun msgInfo(msg: String) {
        //MessageUtils.info(this, msg)
        showCookie(msg, R.color.infoColor)
    }

    /**
     * Muestra un mensaje de advertencia en forma de cookie.
     *
     * @param msg El mensaje a mostrar.
     */
    fun msgWarning(msg: String) {
        //MessageUtils.warning(this, msg)
        showCookie(msg, R.color.warningColor)
    }

    /**
     * Muestra un mensaje de error en forma de cookie.
     *
     * @param msg El mensaje a mostrar.
     */
    fun msgError(msg: String) {
        //MessageUtils.error(this, msg)
        showCookie(msg, R.color.errorColor)
    }

    /**
     * Muestra un mensaje tipo cookie con un fondo personalizado.
     *
     * @param msg El mensaje a mostrar.
     * @param backgroundColor El color de fondo del mensaje.
     */
    private fun showCookie(msg: String, backgroundColor: Int) {
        CookieBar.build(this)
            .setCustomView(R.layout.custom_toast_top)
            .setCustomViewInitializer {
                val tvMessage: TextView = it.findViewById(R.id.tv_message)
                tvMessage.isSelected = true
            }
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(msg)
            .setBackgroundColor(backgroundColor)
            .setDuration(3000)
            .show()
    }

    /**
     * Inicia una nueva actividad y opcionalmente finaliza la actividad actual y anima la transición.
     *
     * @param intent El intent para iniciar la nueva actividad.
     * @param finishExisting Si se debe finalizar la actividad actual.
     * @param animate Si se debe animar la transición.
     */
    @Suppress("SameParameterValue")
    protected open fun startNewActivity(
        intent: Intent,
        finishExisting: Boolean,
        animate: Boolean = true
    ) {
        startActivity(intent)
        if (finishExisting) finish()
        if (animate) animateActivity()
    }

    /**
     * Inicia una nueva actividad sin finalizar la actividad actual.
     *
     * @param intent El intent para iniciar la nueva actividad.
     */
    protected open fun startNewActivity(intent: Intent) {
        startNewActivity(intent, finishExisting = false, animate = true)
    }

    /**
     * Anima la transición entre actividades si está habilitado.
     */
    open fun animateActivity() {
        @Suppress("DEPRECATION")
        if (BuildConfig.EnableAnim) overridePendingTransition(
            R.anim.activity_in,
            R.anim.activity_out
        )
    }

    /**
     * Finaliza la actividad actual con animación si está habilitado.
     *
     * @param animate Si se debe animar la transición de salida.
     */
    @Suppress("SameParameterValue")
    protected open fun finish(animate: Boolean) {
        finish()
        @Suppress("DEPRECATION")
        if (BuildConfig.EnableAnim && animate) overridePendingTransition(
            R.anim.activity_back_in,
            R.anim.activity_back_out
        )
    }

    /**
     * Maneja el evento de presión del botón de retroceso con animación opcional.
     *
     * @param animate Si se debe animar la transición al retroceder.
     */
    @Suppress("DEPRECATION")
    protected open fun onBackPressed(animate: Boolean) {
        super.onBackPressed()
        if (BuildConfig.EnableAnim && animate) overridePendingTransition(
            R.anim.activity_back_in,
            R.anim.activity_back_out
        )
    }

    /**
     * Se llama cuando la actividad es reanudada. Inicializa el delegado de localización.
     */
    public override fun onResume() {
        super.onResume()
        localizationDelegate.onResume(this)
    }

    /**
     * Aplica la configuración de localización al contexto base de la actividad.
     *
     * @param newBase El contexto base para la actividad.
     */
    override fun attachBaseContext(newBase: Context) {
        val localeUpdatedContext = localizationDelegate.attachBaseContext(newBase)
        super.attachBaseContext(localeUpdatedContext)
    }

    /**
     * Obtiene los recursos con soporte para localización.
     *
     * @return Los recursos de la aplicación.
     */
    override fun getResources(): Resources {
        return localizationDelegate.getResources(super.getResources())
    }

    // Métodos de evento de cambio de idioma
    override fun onBeforeLocaleChanged() {}
    override fun onAfterLocaleChanged() {}

    /**
     * Muestra un diálogo de carga personalizado.
     *
     * @param blurView La vista sobre la cual se aplicará el desenfoque.
     * @param image El recurso de imagen a mostrar en el diálogo.
     * @param message El mensaje a mostrar en el diálogo.
     */
    fun showLoaderDialog(
        blurView: ViewGroup, image: Int = R.drawable.ic_home, message: String = ""
    ) {
        try {
            if (dialogLoader?.isShowing == true) {
                dialogLoader?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (dialogLoader == null) {
            dialogLoader = BaseCustomDialog(this, R.layout.dialog_loader, object : BaseCustomDialog.Listener {
                override fun onViewClick(view: View) {
                    if (view.id != 0) {
                        when (view.id) {
                            R.id.txtCancel -> {
                                dialogLoader?.dismiss()
                                finish(true)
                            }
                        }
                    }
                }
            })
        }
        dialogLoader?.getBinding()?.imageView?.setImageResource(image)
        if (message.isNotEmpty()) {
            dialogLoader?.getBinding()?.txtDesc?.text = message
        }
        dialogLoader?.show()
        dialogLoader?.setCancelable(false)
        dialogLoader?.setCanceledOnTouchOutside(false)

        if (dialogLoader?.window != null) {
            val lWindowParams = WindowManager.LayoutParams()
            lWindowParams.copyFrom((dialogLoader?.window ?: return).attributes)
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
            (dialogLoader?.window ?: return).attributes = lWindowParams
        }
    }

    /**
     * Desvanece el diálogo de carga si está visible.
     */
    fun dismissLoaderDialog() {
        try {
            if (dialogLoader != null && dialogLoader?.isShowing == true) {
                dialogLoader?.dismiss()
                dialogLoader = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}