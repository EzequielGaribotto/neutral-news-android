package com.example.neutralnews_android.di.view

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.neutralnews_android.BuildConfig
import com.example.neutralnews_android.R
import com.example.neutralnews_android.databinding.DialogLoaderBinding
import com.example.neutralnews_android.di.dialog.BaseCustomDialog
import com.example.neutralnews_android.util.message.MessageUtils
import org.aviran.cookiebar2.CookieBar

/**
 * Fragmento base que integra Dagger para inyección de dependencias.
 * Extiende AppFragment y proporciona funcionalidad para recibir dependencias
 * inyectadas a través del componente Dagger de la aplicación.
 */
abstract class AppFragment : Fragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private var dialogLoader: BaseCustomDialog<DialogLoaderBinding>? = null

    /**
     * Muestra un mensaje normal al usuario.
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgNormal(msg: String) {
        MessageUtils.normal(requireContext(), msg)
    }

    /**
     * Muestra un mensaje de éxito al usuario.
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgSuccess(msg: String) {
        showCookie(msg, R.color.successColor)
    }

    /**
     * Muestra un mensaje informativo al usuario.
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgInfo(msg: String) {
        showCookie(msg, R.color.infoColor)
    }

    /**
     * Muestra un mensaje de advertencia al usuario.
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgWarning(msg: String) {
        showCookie(msg, R.color.warningColor)
    }

    /**
     * Muestra un mensaje de error al usuario.
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgError(msg: String) {
        showCookie(msg, R.color.errorColor)
    }

    /**
     * Muestra un mensaje utilizando un cookie en la parte superior de la pantalla.
     * @param msg El mensaje que se va a mostrar.
     * @param backgroundColor El color de fondo del mensaje.
     */
    private fun showCookie(msg: String, backgroundColor: Int) {
        CookieBar.build(activity)
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
     * Muestra un diálogo de carga personalizado.
     * @param blurView Vista que se utiliza para el fondo difuso.
     * @param image Imagen que se muestra en el diálogo de carga.
     * @param message Mensaje opcional que se puede mostrar en el diálogo de carga.
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
            dialogLoader = BaseCustomDialog(requireActivity(), R.layout.dialog_loader, object : BaseCustomDialog.Listener {
                override fun onViewClick(view: View) {
                    if (view.id != 0) {
                        when (view.id) {
                            R.id.txtCancel -> {
                                dialogLoader?.dismiss()
                            }
                        }
                    }
                }
            })
        }
        dialogLoader?.getBinding()?.imageView?.setImageResource(image)
        dialogLoader?.show()
        dialogLoader?.setCancelable(false)
        dialogLoader?.setCanceledOnTouchOutside(false)
        if (message.isNotEmpty()) {
            dialogLoader?.getBinding()?.txtDesc?.text = message
        }

        if (dialogLoader?.window != null) {
            val lWindowParams = WindowManager.LayoutParams()
            lWindowParams.copyFrom((dialogLoader?.window ?: return).attributes)
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
            (dialogLoader?.window ?: return).attributes = lWindowParams
        }
    }

    /**
     * Desmonta y oculta el diálogo de carga personalizado.
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

    /**
     * Inicia una nueva actividad con una transición animada.
     * @param intent El intento que especifica la actividad a iniciar.
     * @param finishExisting Indica si se debe finalizar la actividad actual.
     */
    protected open fun startNewActivity(intent: Intent, finishExisting: Boolean) {
        try {
            startActivity(intent)
            if (finishExisting) requireActivity().finish()
            @Suppress("DEPRECATION")
            if (BuildConfig.EnableAnim) requireActivity().overridePendingTransition(
                R.anim.activity_in,
                R.anim.activity_out
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Inicia una nueva actividad con la opción de finalizar la actividad actual y animar la transición.
     * @param intent El intento que especifica la actividad a iniciar.
     * @param finishExisting Indica si se debe finalizar la actividad actual.
     * @param animate Indica si se debe aplicar una animación en la transición.
     */
    @Suppress("SameParameterValue")
    protected open fun startNewActivity(
        intent: Intent,
        finishExisting: Boolean = false,
        animate: Boolean = true
    ) {
        try {
            startActivity(intent)
            if (finishExisting) requireActivity().finish()
            if (BuildConfig.EnableAnim && animate) animateActivity()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Inicia una nueva actividad con una animación predeterminada.
     * @param intent El intento que especifica la actividad a iniciar.
     */
    protected open fun startNewActivity(intent: Intent) {
        startNewActivity(intent, finishExisting = false, animate = true)
    }

    /**
     * Aplica la animación de transición para la actividad.
     */
    open fun animateActivity() {
        @Suppress("DEPRECATION")
        if (BuildConfig.EnableAnim) requireActivity().overridePendingTransition(
            R.anim.activity_in,
            R.anim.activity_out
        )
    }
}