// BaseViewModel.kt
package com.example.neutralnews_android.di.viewmodel

import android.app.Application
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.lifecycle.AndroidViewModel
import com.example.neutralnews_android.di.NeutralNewsApplication
import com.example.neutralnews_android.di.event.SingleLiveEvent
import com.example.neutralnews_android.util.message.MessageUtils
import io.reactivex.disposables.CompositeDisposable

/**
 * Clase base para todos los ViewModels de la aplicación.
 * Proporciona funcionalidades comunes como el manejo de clics, mensajes personalizados y disposables.
 */
abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Componente para gestionar disposables de RxJava.
     * Se utiliza para limpiar las suscripciones al ser necesario.
     */
    @JvmField
    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    /**
     * Evento de un solo uso para manejar clics en vistas.
     * Se utiliza para disparar eventos cuando un usuario hace clic en una vista.
     */
    @JvmField
    val obrClick: SingleLiveEvent<View> = SingleLiveEvent()

    /**
     * Maneja los clics de una vista y realiza la retroalimentación háptica.
     * Llama al evento `obrClick` y realiza la vibración asociada al clic.
     *
     * @param view La vista sobre la que se realizó el clic.
     */
    fun onClick(view: View) {
        obrClick.value = view
        @Suppress("DEPRECATION") view.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            // Ignora la configuración global del dispositivo para la retroalimentación háptica.
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Obtiene el mensaje de error en caso de una excepción de red.
     * Utiliza un manejador de errores de red para obtener el mensaje adecuado.
     *
     * @param e La excepción que contiene el error.
     * @return El mensaje de error correspondiente.
     */
    fun getNetworkMsg(e: Exception): String {
        return NeutralNewsApplication.instance.networkErrorHandler?.getErrMsg(e) ?: ""
    }

    /**
     * Limpia los recursos y disposables cuando el ViewModel es destruido.
     * Se llama automáticamente cuando el ViewModel es destruido.
     */
    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }

    /**
     * Muestra un mensaje de tipo normal en la aplicación.
     *
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgNormal(msg: String) {
        MessageUtils.normal(NeutralNewsApplication.applicationContext(), msg)
    }

    /**
     * Muestra un mensaje de tipo éxito en la aplicación.
     *
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgSuccess(msg: String) {
        MessageUtils.success(NeutralNewsApplication.applicationContext(), msg)
    }

    /**
     * Muestra un mensaje informativo en la aplicación.
     *
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgInfo(msg: String) {
        MessageUtils.info(NeutralNewsApplication.applicationContext(), msg)
    }

    /**
     * Muestra un mensaje de advertencia en la aplicación.
     *
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgWarning(msg: String) {
        MessageUtils.warning(NeutralNewsApplication.applicationContext(), msg)
    }

    /**
     * Muestra un mensaje de error en la aplicación.
     *
     * @param msg El mensaje que se va a mostrar.
     */
    fun msgError(msg: String) {
        MessageUtils.error(NeutralNewsApplication.applicationContext(), msg)
    }
}
